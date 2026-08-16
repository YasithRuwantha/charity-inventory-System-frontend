package com.charitymanagement.api.charitymanagementback.donation.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.CreateDonationRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.BulkDonationImportResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.BulkDonationPreviewResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationItemResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationReceiptResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationResponse;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import com.charitymanagement.api.charitymanagementback.donation.service.BulkDonationService;
import com.charitymanagement.api.charitymanagementback.donation.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Donation intake, history, receipts and bulk upload")
public class DonationController {

    private final DonationService donationService;
    private final BulkDonationService bulkDonationService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Register a donation",
            description = "Adds every item line to stock and writes a DONATION_IN ledger entry per line, all in "
                    + "one transaction. Send an Idempotency-Key header to make a retry safe.")
    public ResponseEntity<ApiResponse<DonationResponse>> create(
            @Valid @RequestBody CreateDonationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(donationService.create(request, idempotencyKey),
                        "Donation registered and inventory updated successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Search donation history",
            description = "Filter by text, donor, status and date range. Item lines are included.")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long donorId,
            @RequestParam(required = false) DonationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DonationResponse> page =
                donationService.search(search, donorId, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/bulk/template")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Download the bulk upload CSV template")
    public ResponseEntity<byte[]> template() {
        byte[] body = bulkDonationService.template().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"donation-bulk-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @PostMapping(value = "/bulk/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Validate a bulk upload without saving",
            description = "Parses the CSV and reports every row with its validation errors. Writes nothing.")
    public ResponseEntity<ApiResponse<BulkDonationPreviewResponse>> preview(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(bulkDonationService.preview(file),
                "File validated; no records were saved"));
    }

    @PostMapping(value = "/bulk/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Import a bulk donation file",
            description = "By default a single invalid row aborts the entire import. Pass allowPartial=true to "
                    + "import the valid rows and receive the rejected ones in the response.")
    public ResponseEntity<ApiResponse<BulkDonationImportResponse>> importFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean allowPartial,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        BulkDonationImportResponse result = bulkDonationService.importFile(file, allowPartial, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Look up a donation by its reference (DON-2026-000001)")
    public ResponseEntity<ApiResponse<DonationResponse>> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success(donationService.getByReference(reference)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Get one donation with its items")
    public ResponseEntity<ApiResponse<DonationResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(donationService.get(id)));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Item lines of a donation")
    public ResponseEntity<ApiResponse<List<DonationItemResponse>>> items(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(donationService.getItems(id)));
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Donation receipt",
            description = "Self-contained payload shaped so a PDF renderer needs no further lookups.")
    public ResponseEntity<ApiResponse<DonationReceiptResponse>> receipt(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(donationService.getReceipt(id)));
    }
}
