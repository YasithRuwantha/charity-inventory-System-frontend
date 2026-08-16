package com.charitymanagement.api.charitymanagementback.donation.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.reference.ReferenceGenerator;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.CreateDonationRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.DonationItemRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationItemResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationReceiptResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonorStatisticsResponse;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donation;
import com.charitymanagement.api.charitymanagementback.donation.entity.DonationItem;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donor;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import com.charitymanagement.api.charitymanagementback.donation.mapper.DonationMapper;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonationItemRepository;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonationRepository;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonationSpecifications;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryItemService;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryStockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

    private static final Set<String> SORTABLE =
            Set.of("id", "donationReference", "donationDate", "status", "createdAt", "updatedAt");

    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final DonorService donorService;
    private final InventoryItemService inventoryItemService;
    private final InventoryStockService stockService;
    private final DonationMapper mapper;
    private final ReferenceGenerator referenceGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    /**
     * Registers a donation and adds every line to stock.
     *
     * <p>Runs in a single transaction: if any line fails validation or the stock update, the whole
     * donation — header, items and ledger entries — is rolled back. Partially-applied donations are
     * not possible.
     *
     * @param idempotencyKey optional {@code Idempotency-Key} header; a repeat of the same key
     *                       returns the original donation instead of creating a second one
     */
    @Transactional
    public DonationResponse create(CreateDonationRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = donationRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                log.info("Returning existing donation {} for idempotency key",
                        existing.get().getDonationReference());
                return toResponse(existing.get());
            }
        }

        Donor donor = donorService.requireDonor(request.getDonorId());
        if (donor.getStatus() != RecordStatus.ACTIVE) {
            throw new BadRequestException("Donor " + donor.getDonorCode() + " is inactive", "DONOR_INACTIVE");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("A donation must contain at least one item");
        }
        User actor = currentUserProvider.requireManagedUser();

        Donation donation = donationRepository.save(Donation.builder()
                .donationReference(referenceGenerator.nextYearly(ReferenceGenerator.DONATION,
                        request.getDonationDate().getYear()))
                .donor(donor)
                .donationDate(request.getDonationDate())
                .notes(trimToNull(request.getNotes()))
                .receivedBy(actor)
                .status(DonationStatus.RECEIVED)
                .idempotencyKey(trimToNull(idempotencyKey))
                .build());

        List<DonationItem> savedItems = new ArrayList<>();
        long totalQuantity = 0;
        for (DonationItemRequest itemRequest : request.getItems()) {
            InventoryItem inventoryItem = inventoryItemService.requireItem(itemRequest.getInventoryItemId());
            if (inventoryItem.isArchived()) {
                throw new BadRequestException("Inventory item " + inventoryItem.getItemCode()
                        + " is archived and cannot receive donations", "ITEM_ARCHIVED");
            }
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for item "
                        + inventoryItem.getItemName());
            }

            DonationItem item = donationItemRepository.save(DonationItem.builder()
                    .donation(donation)
                    .inventoryItem(inventoryItem)
                    .quantity(itemRequest.getQuantity())
                    .expiryDate(itemRequest.getExpiryDate())
                    .notes(trimToNull(itemRequest.getNotes()))
                    .build());

            stockService.increase(inventoryItem.getId(), itemRequest.getQuantity(),
                    TransactionType.DONATION_IN, ReferenceType.DONATION, donation.getId(),
                    "Donation " + donation.getDonationReference(), actor, itemRequest.getExpiryDate());

            savedItems.add(item);
            totalQuantity += itemRequest.getQuantity();
        }

        auditService.record(AuditAction.REGISTER_DONATION, "Donation", donation.getId(),
                "Registered donation " + donation.getDonationReference() + " from " + donor.getDonorName(),
                null, Map.of(
                        "donationReference", donation.getDonationReference(),
                        "donorId", donor.getId(),
                        "donationDate", donation.getDonationDate(),
                        "itemLines", savedItems.size(),
                        "totalQuantity", totalQuantity));

        log.info("Registered donation {} from donor {} with {} item lines ({} units)",
                donation.getDonationReference(), donor.getDonorCode(), savedItems.size(), totalQuantity);
        return mapper.toResponse(donation, savedItems);
    }

    @Transactional(readOnly = true)
    public DonationResponse get(Long id) {
        return toResponse(requireDonation(id));
    }

    @Transactional(readOnly = true)
    public DonationResponse getByReference(String reference) {
        Donation donation = donationRepository.findByDonationReferenceIgnoreCase(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Donation", reference));
        return toResponse(donation);
    }

    @Transactional(readOnly = true)
    public List<DonationItemResponse> getItems(Long id) {
        requireDonation(id);
        return donationItemRepository.findByDonationId(id).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DonationReceiptResponse getReceipt(Long id) {
        Donation donation = requireDonation(id);
        return mapper.toReceipt(donation, donationItemRepository.findByDonationId(id));
    }

    @Transactional(readOnly = true)
    public Page<DonationResponse> search(String search,
                                         Long donorId,
                                         DonationStatus status,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "donationDate");
        return donationRepository
                .findAll(DonationSpecifications.donations(search, donorId, status, startDate, endDate), safe)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DonorStatisticsResponse getDonorStatistics(Long donorId) {
        Donor donor = donorService.requireDonor(donorId);
        Page<DonationResponse> recent = search(null, donorId, DonationStatus.RECEIVED, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "donationDate")));

        return DonorStatisticsResponse.builder()
                .donorId(donor.getId())
                .donorCode(donor.getDonorCode())
                .donorName(donor.getDonorName())
                .totalDonations(donationRepository.countByDonor(donorId, DonationStatus.RECEIVED))
                .totalItemLines(donationRepository.countItemsByDonor(donorId, DonationStatus.RECEIVED))
                .totalQuantityDonated(donationRepository.sumQuantityByDonor(donorId, DonationStatus.RECEIVED))
                .lastDonationDate(donationRepository.findLastDonationDate(donorId, DonationStatus.RECEIVED))
                .recentDonations(recent.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public Donation requireDonation(Long id) {
        return donationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Donation", id));
    }

    private DonationResponse toResponse(Donation donation) {
        return mapper.toResponse(donation, donationItemRepository.findByDonationId(donation.getId()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
