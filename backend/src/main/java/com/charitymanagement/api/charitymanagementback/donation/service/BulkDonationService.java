package com.charitymanagement.api.charitymanagementback.donation.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.CreateDonationRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.DonationItemRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.BulkDonationImportResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.BulkDonationPreviewResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.BulkRowResult;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationResponse;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donor;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV bulk donation upload.
 *
 * <p>Import rules are deliberately strict: donors and inventory items must already exist, and any
 * Category/Unit supplied must match the item on record. A spreadsheet cannot silently create master
 * data or reclassify stock.
 *
 * <p>Rows sharing a donor and donation date are merged into one donation with several item lines.
 */
@Service
@RequiredArgsConstructor
public class BulkDonationService {

    private static final Logger log = LoggerFactory.getLogger(BulkDonationService.class);

    public static final String TEMPLATE_HEADER = "Donor,Item,Category,Quantity,Unit,Expiry Date,Donation Date";

    private static final String COL_DONOR = "Donor";
    private static final String COL_ITEM = "Item";
    private static final String COL_CATEGORY = "Category";
    private static final String COL_QUANTITY = "Quantity";
    private static final String COL_UNIT = "Unit";
    private static final String COL_EXPIRY = "Expiry Date";
    private static final String COL_DONATION_DATE = "Donation Date";

    private static final int MAX_ROWS = 5000;

    private final DonorService donorService;
    private final InventoryItemService itemService;
    private final DonationService donationService;
    private final AuditService auditService;

    /** Parses and validates without writing anything. */
    @Transactional(readOnly = true)
    public BulkDonationPreviewResponse preview(MultipartFile file) {
        List<ParsedRow> rows = parse(file);
        List<BulkRowResult> results = rows.stream().map(ParsedRow::result).toList();

        long valid = results.stream().filter(BulkRowResult::isValid).count();
        return BulkDonationPreviewResponse.builder()
                .fileName(file.getOriginalFilename())
                .totalRows(results.size())
                .validRows((int) valid)
                .invalidRows(results.size() - (int) valid)
                .donationsToCreate(groupValidRows(rows).size())
                .rows(results)
                .build();
    }

    /**
     * @param allowPartial when false (the default) a single invalid row aborts the whole import and
     *                     nothing is written; when true the valid rows are imported and the rest are
     *                     reported back
     */
    @Transactional
    public BulkDonationImportResponse importFile(MultipartFile file, boolean allowPartial, String idempotencyKey) {
        List<ParsedRow> rows = parse(file);
        List<BulkRowResult> invalid = rows.stream().map(ParsedRow::result)
                .filter(result -> !result.isValid())
                .toList();

        if (!invalid.isEmpty() && !allowPartial) {
            return BulkDonationImportResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .totalRows(rows.size())
                    .successfulRows(0)
                    .failedRows(invalid.size())
                    .skippedRows(rows.size() - invalid.size())
                    .createdDonations(0)
                    .totalQuantityImported(0)
                    .createdDonationReferences(List.of())
                    .errors(invalid)
                    .message("Import aborted: " + invalid.size() + " row(s) failed validation and nothing was "
                            + "written. Fix the rows listed below, or retry with allowPartial=true to import "
                            + "only the valid rows.")
                    .build();
        }

        Map<GroupKey, List<ParsedRow>> grouped = groupValidRows(rows);
        List<String> references = new ArrayList<>();
        long totalQuantity = 0;
        int importedRows = 0;

        for (Map.Entry<GroupKey, List<ParsedRow>> group : grouped.entrySet()) {
            GroupKey key = group.getKey();
            CreateDonationRequest request = new CreateDonationRequest();
            request.setDonorId(key.donorId());
            request.setDonationDate(key.donationDate());
            request.setNotes("Bulk import from " + file.getOriginalFilename());
            request.setItems(group.getValue().stream().map(row -> {
                DonationItemRequest item = new DonationItemRequest();
                item.setInventoryItemId(row.inventoryItemId());
                item.setQuantity(row.quantity());
                item.setExpiryDate(row.expiryDate());
                return item;
            }).toList());

            // Scope the key per donation so a retried upload re-uses the original donations.
            String donationKey = idempotencyKey == null || idempotencyKey.isBlank() ? null
                    : idempotencyKey.trim() + ":" + key.donorId() + ":" + key.donationDate();

            DonationResponse created = donationService.create(request, donationKey);
            references.add(created.getDonationReference());
            totalQuantity += created.getTotalQuantity();
            importedRows += group.getValue().size();
        }

        auditService.record(AuditAction.BULK_IMPORT_DONATIONS, "Donation", null,
                "Bulk imported " + references.size() + " donation(s) from " + file.getOriginalFilename(),
                null, Map.of(
                        "fileName", String.valueOf(file.getOriginalFilename()),
                        "totalRows", rows.size(),
                        "importedRows", importedRows,
                        "failedRows", invalid.size(),
                        "createdDonations", references.size()));

        log.info("Bulk import of {} created {} donation(s) from {} valid row(s); {} row(s) rejected",
                file.getOriginalFilename(), references.size(), importedRows, invalid.size());

        return BulkDonationImportResponse.builder()
                .fileName(file.getOriginalFilename())
                .totalRows(rows.size())
                .successfulRows(importedRows)
                .failedRows(invalid.size())
                .skippedRows(invalid.size())
                .createdDonations(references.size())
                .totalQuantityImported(totalQuantity)
                .createdDonationReferences(references)
                .errors(invalid)
                .message(invalid.isEmpty()
                        ? "All rows imported successfully"
                        : importedRows + " row(s) imported; " + invalid.size() + " row(s) rejected and listed below")
                .build();
    }

    public String template() {
        LocalDate today = LocalDate.now();
        return TEMPLATE_HEADER + "\n"
                + "DONOR-000001,Rice,Food,50,KG," + today.plusYears(1) + "," + today + "\n"
                + "Helping Hands Foundation,Blankets,Clothing,20,ITEM,," + today + "\n";
    }

    // ── parsing ──────────────────────────────────────────────────────────────

    private List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A non-empty CSV file is required", "FILE_REQUIRED");
        }
        String name = file.getOriginalFilename();
        if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BadRequestException(
                    "XLSX uploads are not supported. Export the sheet as CSV and upload that instead.",
                    "UNSUPPORTED_FILE_TYPE");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<ParsedRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = format.parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            assertHeaderPresent(headers, COL_DONOR, COL_ITEM, COL_QUANTITY, COL_DONATION_DATE);

            int rowNumber = 0;
            for (CSVRecord record : parser) {
                rowNumber++;
                if (rowNumber > MAX_ROWS) {
                    throw new BadRequestException("The file exceeds the maximum of " + MAX_ROWS + " rows",
                            "FILE_TOO_LARGE");
                }
                rows.add(validateRow(rowNumber, record, headers));
            }
        } catch (IOException ex) {
            throw new BadRequestException("The CSV file could not be read: " + ex.getMessage(), "FILE_UNREADABLE");
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("The CSV file is malformed: " + ex.getMessage(), "FILE_MALFORMED");
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("The file contains a header but no data rows", "FILE_EMPTY");
        }
        return rows;
    }

    private ParsedRow validateRow(int rowNumber, CSVRecord record, List<String> headers) {
        String donorValue = value(record, headers, COL_DONOR);
        String itemValue = value(record, headers, COL_ITEM);
        String categoryValue = value(record, headers, COL_CATEGORY);
        String quantityValue = value(record, headers, COL_QUANTITY);
        String unitValue = value(record, headers, COL_UNIT);
        String expiryValue = value(record, headers, COL_EXPIRY);
        String donationDateValue = value(record, headers, COL_DONATION_DATE);

        List<String> errors = new ArrayList<>();

        Donor donor = null;
        if (isBlank(donorValue)) {
            errors.add("Donor is required");
        } else {
            donor = donorService.findByNameOrCode(donorValue);
            if (donor == null) {
                errors.add("Unknown donor '" + donorValue + "' — register the donor before importing");
            } else if (donor.getStatus() != RecordStatus.ACTIVE) {
                errors.add("Donor '" + donorValue + "' is inactive");
            }
        }

        InventoryItem item = null;
        if (isBlank(itemValue)) {
            errors.add("Item is required");
        } else {
            List<InventoryItem> matches = itemService.findActiveByName(itemValue);
            boolean categoryMismatch = false;
            if (!isBlank(categoryValue)) {
                List<InventoryItem> byCategory = matches.stream()
                        .filter(candidate -> categoryValue.equalsIgnoreCase(candidate.getCategory().getName()))
                        .toList();
                if (!matches.isEmpty() && byCategory.isEmpty()) {
                    categoryMismatch = true;
                    errors.add("Category '" + categoryValue + "' does not match item '" + itemValue
                            + "', which belongs to '" + matches.get(0).getCategory().getName() + "'");
                }
                matches = byCategory;
            }

            if (matches.isEmpty() && !categoryMismatch) {
                errors.add("Unknown item '" + itemValue + "' — create the inventory item before importing");
            } else if (matches.size() > 1) {
                errors.add("Item '" + itemValue + "' exists in several categories — add a Category column value "
                        + "to identify which one");
            } else if (matches.size() == 1) {
                item = matches.get(0);
                if (!isBlank(unitValue)) {
                    UnitOfMeasure unit = parseEnum(unitValue);
                    if (unit == null) {
                        errors.add("Unknown unit '" + unitValue + "'");
                    } else if (unit != item.getUnit()) {
                        errors.add("Unit '" + unitValue + "' does not match item '" + item.getItemName()
                                + "', which is measured in " + item.getUnit());
                    }
                }
            }
        }

        Integer quantity = null;
        if (isBlank(quantityValue)) {
            errors.add("Quantity is required");
        } else {
            try {
                quantity = Integer.valueOf(quantityValue.trim());
                if (quantity <= 0) {
                    errors.add("Quantity must be greater than zero");
                }
            } catch (NumberFormatException ex) {
                errors.add("Quantity '" + quantityValue + "' is not a whole number");
            }
        }

        LocalDate expiryDate = null;
        if (!isBlank(expiryValue)) {
            expiryDate = parseDate(expiryValue);
            if (expiryDate == null) {
                errors.add("Expiry date '" + expiryValue + "' is not a valid ISO date (yyyy-MM-dd)");
            }
        }

        LocalDate donationDate = null;
        if (isBlank(donationDateValue)) {
            errors.add("Donation date is required");
        } else {
            donationDate = parseDate(donationDateValue);
            if (donationDate == null) {
                errors.add("Donation date '" + donationDateValue + "' is not a valid ISO date (yyyy-MM-dd)");
            } else if (donationDate.isAfter(LocalDate.now())) {
                errors.add("Donation date cannot be in the future");
            }
        }

        BulkRowResult result = BulkRowResult.builder()
                .rowNumber(rowNumber)
                .valid(errors.isEmpty())
                .donor(donorValue)
                .item(itemValue)
                .category(categoryValue)
                .quantity(quantityValue)
                .unit(unitValue)
                .expiryDate(expiryValue)
                .donationDate(donationDateValue)
                .errors(errors)
                .build();

        return new ParsedRow(result,
                donor != null ? donor.getId() : null,
                item != null ? item.getId() : null,
                quantity,
                expiryDate,
                donationDate);
    }

    private Map<GroupKey, List<ParsedRow>> groupValidRows(List<ParsedRow> rows) {
        Map<GroupKey, List<ParsedRow>> grouped = new LinkedHashMap<>();
        for (ParsedRow row : rows) {
            if (!row.result().isValid()) {
                continue;
            }
            grouped.computeIfAbsent(new GroupKey(row.donorId(), row.donationDate()), key -> new ArrayList<>())
                    .add(row);
        }
        return grouped;
    }

    private static void assertHeaderPresent(List<String> headers, String... required) {
        for (String column : required) {
            boolean present = headers.stream().anyMatch(h -> h != null && h.trim().equalsIgnoreCase(column));
            if (!present) {
                throw new BadRequestException("The CSV file is missing the required '" + column + "' column. "
                        + "Expected header: " + TEMPLATE_HEADER, "MISSING_COLUMN");
            }
        }
    }

    private static String value(CSVRecord record, List<String> headers, String column) {
        for (String header : headers) {
            if (header != null && header.trim().equalsIgnoreCase(column) && record.isMapped(header)) {
                String raw = record.get(header);
                return raw == null ? null : raw.trim();
            }
        }
        return null;
    }

    private static UnitOfMeasure parseEnum(String value) {
        try {
            return UnitOfMeasure.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ParsedRow(BulkRowResult result,
                             Long donorId,
                             Long inventoryItemId,
                             Integer quantity,
                             LocalDate expiryDate,
                             LocalDate donationDate) {
    }

    private record GroupKey(Long donorId, LocalDate donationDate) {
    }
}
