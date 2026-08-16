package com.charitymanagement.api.charitymanagementback.distribution.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.response.BeneficiaryStatisticsResponse;
import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.beneficiary.service.BeneficiaryService;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.exception.DuplicateDistributionException;
import com.charitymanagement.api.charitymanagementback.common.exception.ExpiredInventoryException;
import com.charitymanagement.api.charitymanagementback.common.exception.ForbiddenException;
import com.charitymanagement.api.charitymanagementback.common.exception.InsufficientStockException;
import com.charitymanagement.api.charitymanagementback.common.exception.InvalidDistributionException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.reference.ReferenceGenerator;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.AllocateDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.ApproveDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.CancelDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.CompleteDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.CreateDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.DistributionItemRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.RejectDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.UpdateDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionReportResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DuplicateDistributionResponse;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionItem;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionOverride;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import com.charitymanagement.api.charitymanagementback.distribution.mapper.DistributionMapper;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionItemRepository;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionOverrideRepository;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionRequestRepository;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionSpecifications;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns the aid-distribution lifecycle.
 *
 * <p>The rule that shapes this class: inventory moves once, at completion. Creating, allocating
 * and approving a request only record intent, so an abandoned request never strands stock. Every
 * state change runs in a single transaction with the stock movements it triggers, which is what
 * makes a multi-item completion all-or-nothing.
 */
@Service
@RequiredArgsConstructor
public class DistributionService {

    private static final Logger log = LoggerFactory.getLogger(DistributionService.class);

    private static final Set<String> SORTABLE = Set.of(
            "id", "requestReference", "requestDate", "priority", "status", "completedAt",
            "createdAt", "updatedAt");

    private final DistributionRequestRepository distributionRepository;
    private final DistributionItemRepository distributionItemRepository;
    private final DistributionOverrideRepository overrideRepository;
    private final BeneficiaryService beneficiaryService;
    private final InventoryItemService inventoryItemService;
    private final InventoryStockService stockService;
    private final DuplicateDistributionChecker duplicateChecker;
    private final DistributionMapper mapper;
    private final ReferenceGenerator referenceGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    // ── Creation and editing ────────────────────────────────────────────────

    @Transactional
    public DistributionResponse create(CreateDistributionRequest request) {
        Beneficiary beneficiary = beneficiaryService.requireBeneficiary(request.getBeneficiaryId());
        assertBeneficiaryActive(beneficiary);
        User actor = currentUserProvider.requireManagedUser();

        DistributionRequest distribution = distributionRepository.save(DistributionRequest.builder()
                .requestReference(referenceGenerator.nextYearly(ReferenceGenerator.DISTRIBUTION,
                        request.getRequestDate().getYear()))
                .beneficiary(beneficiary)
                .requestDate(request.getRequestDate())
                .priority(request.getPriority() != null ? request.getPriority() : beneficiary.getPriorityLevel())
                .reason(trimToNull(request.getReason()))
                .notes(trimToNull(request.getNotes()))
                .status(DistributionStatus.PENDING)
                .requestedBy(actor)
                .build());

        List<DistributionItem> items = createItems(distribution, request.getItems());

        auditService.record(AuditAction.CREATE_DISTRIBUTION, "DistributionRequest", distribution.getId(),
                "Created distribution " + distribution.getRequestReference() + " for "
                        + beneficiary.getBeneficiaryName(),
                null, Map.of(
                        "requestReference", distribution.getRequestReference(),
                        "beneficiaryId", beneficiary.getId(),
                        "requestDate", distribution.getRequestDate(),
                        "priority", distribution.getPriority(),
                        "itemLines", items.size()));

        log.info("Created distribution {} for beneficiary {} with {} item lines",
                distribution.getRequestReference(), beneficiary.getBeneficiaryCode(), items.size());
        return mapper.toResponse(distribution, items);
    }

    @Transactional
    public DistributionResponse update(Long id, UpdateDistributionRequest request) {
        DistributionRequest distribution = requireRequest(id);
        if (!distribution.isEditable()) {
            throw new InvalidDistributionException("Only a PENDING distribution can be edited; "
                    + distribution.getRequestReference() + " is " + distribution.getStatus());
        }
        Map<String, Object> before = snapshot(distribution);

        distribution.setRequestDate(request.getRequestDate());
        distribution.setPriority(request.getPriority());
        distribution.setReason(trimToNull(request.getReason()));
        distribution.setNotes(trimToNull(request.getNotes()));
        distributionRepository.save(distribution);

        // The line-up is replaced wholesale; nothing has moved stock yet, so this is safe.
        distributionItemRepository.deleteAll(distributionItemRepository.findByDistributionRequestId(id));
        distributionItemRepository.flush();
        List<DistributionItem> items = createItems(distribution, request.getItems());

        auditService.record(AuditAction.UPDATE_DISTRIBUTION, "DistributionRequest", id,
                "Updated distribution " + distribution.getRequestReference(), before, snapshot(distribution));
        return mapper.toResponse(distribution, items);
    }

    // ── Workflow transitions ────────────────────────────────────────────────

    /**
     * Earmarks stock against each line. Deliberately does not touch inventory: a reservation model
     * would let abandoned requests hold stock hostage, so availability is re-checked at completion.
     */
    @Transactional
    public DistributionResponse allocate(Long id, AllocateDistributionRequest request) {
        DistributionRequest distribution = requireRequest(id);
        if (distribution.getStatus() != DistributionStatus.PENDING
                && distribution.getStatus() != DistributionStatus.APPROVED) {
            throw new InvalidDistributionException("Cannot allocate against a "
                    + distribution.getStatus() + " distribution");
        }

        Map<Long, DistributionItem> byId = itemsById(id);
        Map<String, Object> before = allocationSnapshot(byId.values());

        for (AllocateDistributionRequest.AllocationLine line : request.getItems()) {
            DistributionItem item = requireLine(byId, line.getDistributionItemId(), distribution);
            int allocated = line.getAllocatedQuantity();

            if (allocated > item.getRequestedQuantity()) {
                throw new BadRequestException("Cannot allocate " + allocated + " of "
                        + item.getInventoryItem().getItemName() + " when only "
                        + item.getRequestedQuantity() + " was requested", "ALLOCATION_EXCEEDS_REQUEST");
            }
            InventoryItem inventoryItem = item.getInventoryItem();
            if (allocated > 0 && inventoryItem.isExpired()) {
                throw new ExpiredInventoryException("Item " + inventoryItem.getItemName() + " ("
                        + inventoryItem.getItemCode() + ") expired on " + inventoryItem.getExpiryDate()
                        + " and cannot be allocated");
            }
            int available = inventoryItem.getQuantity() == null ? 0 : inventoryItem.getQuantity();
            if (available < allocated) {
                throw InsufficientStockException.forItem(inventoryItem.getItemCode(),
                        inventoryItem.getItemName(), available, allocated);
            }
            item.setAllocatedQuantity(allocated);
        }
        List<DistributionItem> items = distributionItemRepository.saveAll(byId.values());

        auditService.record(AuditAction.ALLOCATE_DISTRIBUTION, "DistributionRequest", id,
                "Allocated inventory for distribution " + distribution.getRequestReference(),
                before, allocationSnapshot(items));
        return mapper.toResponse(distribution, items);
    }

    @Transactional
    public DistributionResponse approve(Long id, ApproveDistributionRequest request) {
        DistributionRequest distribution = requireRequest(id);
        requireStatus(distribution, DistributionStatus.PENDING, "approved");
        assertBeneficiaryActive(distribution.getBeneficiary());

        List<DistributionItem> items = distributionItemRepository.findByDistributionRequestId(id);
        boolean nothingAllocated = items.stream()
                .allMatch(item -> item.getAllocatedQuantity() == null || item.getAllocatedQuantity() <= 0);
        if (nothingAllocated) {
            throw new InvalidDistributionException(
                    "Allocate inventory against distribution " + distribution.getRequestReference()
                            + " before approving it", "ALLOCATION_REQUIRED");
        }

        distribution.setStatus(DistributionStatus.APPROVED);
        distribution.setApprovedBy(currentUserProvider.requireManagedUser());
        distribution.setApprovedAt(LocalDateTime.now());
        appendNotes(distribution, request != null ? request.getNotes() : null);
        distributionRepository.save(distribution);

        auditService.record(AuditAction.APPROVE_DISTRIBUTION, "DistributionRequest", id,
                "Approved distribution " + distribution.getRequestReference(),
                Map.of("status", DistributionStatus.PENDING),
                Map.of("status", DistributionStatus.APPROVED));

        // Worth stating explicitly in the log: approval is not a stock movement.
        log.info("Approved distribution {} (inventory unchanged until completion)",
                distribution.getRequestReference());
        return mapper.toResponse(distribution, items);
    }

    @Transactional
    public DistributionResponse reject(Long id, RejectDistributionRequest request) {
        DistributionRequest distribution = requireRequest(id);
        if (distribution.getStatus() != DistributionStatus.PENDING
                && distribution.getStatus() != DistributionStatus.APPROVED) {
            throw new InvalidDistributionException("Cannot reject a " + distribution.getStatus()
                    + " distribution");
        }
        DistributionStatus previous = distribution.getStatus();
        distribution.setStatus(DistributionStatus.REJECTED);
        distribution.setRejectedBy(currentUserProvider.requireManagedUser());
        distribution.setRejectedAt(LocalDateTime.now());
        distribution.setRejectionReason(request.getReason().trim());
        distributionRepository.save(distribution);

        auditService.record(AuditAction.REJECT_DISTRIBUTION, "DistributionRequest", id,
                "Rejected distribution " + distribution.getRequestReference(),
                Map.of("status", previous),
                Map.of("status", DistributionStatus.REJECTED, "reason", distribution.getRejectionReason()));
        return toResponse(distribution);
    }

    @Transactional
    public DistributionResponse cancel(Long id, CancelDistributionRequest request) {
        DistributionRequest distribution = requireRequest(id);
        if (distribution.getStatus() == DistributionStatus.COMPLETED) {
            throw new InvalidDistributionException("Distribution " + distribution.getRequestReference()
                    + " is already completed and cannot be cancelled", "DISTRIBUTION_ALREADY_COMPLETED");
        }
        if (distribution.getStatus().isTerminal()) {
            throw new InvalidDistributionException("Cannot cancel a " + distribution.getStatus()
                    + " distribution");
        }
        DistributionStatus previous = distribution.getStatus();
        distribution.setStatus(DistributionStatus.CANCELLED);
        distribution.setCancelledBy(currentUserProvider.requireManagedUser());
        distribution.setCancelledAt(LocalDateTime.now());
        distribution.setCancellationReason(request.getReason().trim());
        distributionRepository.save(distribution);

        auditService.record(AuditAction.CANCEL_DISTRIBUTION, "DistributionRequest", id,
                "Cancelled distribution " + distribution.getRequestReference(),
                Map.of("status", previous),
                Map.of("status", DistributionStatus.CANCELLED, "reason", distribution.getCancellationReason()));
        return toResponse(distribution);
    }

    /**
     * Confirms the hand-over and is the only path that reduces stock.
     *
     * <p>The request row is locked first so two concurrent calls cannot both see APPROVED, and each
     * inventory row is locked by {@link InventoryStockService}. Every line must succeed: if the last
     * item is short, expired or a blocked duplicate, the whole transaction rolls back and the
     * earlier deductions are undone — no partial distributions.
     */
    @Transactional
    public DistributionResponse complete(Long id, CompleteDistributionRequest request) {
        DistributionRequest distribution = distributionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution request", id));

        if (distribution.getStatus() == DistributionStatus.COMPLETED) {
            throw new InvalidDistributionException("Distribution " + distribution.getRequestReference()
                    + " has already been completed", "DISTRIBUTION_ALREADY_COMPLETED");
        }
        requireStatus(distribution, DistributionStatus.APPROVED, "completed");

        Beneficiary beneficiary = distribution.getBeneficiary();
        assertBeneficiaryActive(beneficiary);

        User actor = currentUserProvider.requireManagedUser();
        boolean overrideRequested = request != null && request.isOverrideDuplicates();
        String overrideReason = request != null ? trimToNull(request.getOverrideReason()) : null;
        if (overrideRequested) {
            if (!currentUserProvider.isAdmin()) {
                throw new ForbiddenException("Only an ADMIN may override a duplicate-distribution warning");
            }
            if (overrideReason == null) {
                throw new BadRequestException("An override reason is required to bypass a "
                        + "duplicate-distribution warning", "OVERRIDE_REASON_REQUIRED");
            }
        }

        Map<Long, DistributionItem> byId = itemsById(id);
        Map<Long, Integer> quantities = resolveDistributedQuantities(distribution, byId, request);

        for (DistributionItem item : byId.values()) {
            int quantity = quantities.get(item.getId());
            InventoryItem inventoryItem = item.getInventoryItem();

            DuplicateDistributionResponse duplicate = duplicateChecker.check(beneficiary, inventoryItem);
            if (duplicate.isDuplicateWarning()) {
                if (!overrideRequested) {
                    throw new DuplicateDistributionException(
                            beneficiary.getBeneficiaryName() + " received " + inventoryItem.getItemName()
                                    + " " + duplicate.getDaysSinceLastDistribution()
                                    + " day(s) ago. An ADMIN override with a reason is required to proceed.",
                            duplicate);
                }
                recordOverride(distribution, beneficiary, inventoryItem, duplicate, overrideReason, actor);
            }

            // Locks the inventory row, refuses expired stock and refuses to go negative.
            stockService.decrease(inventoryItem.getId(), quantity, TransactionType.DISTRIBUTION_OUT,
                    ReferenceType.DISTRIBUTION, distribution.getId(),
                    "Distribution " + distribution.getRequestReference(), actor, true);

            item.setDistributedQuantity(quantity);
        }
        List<DistributionItem> items = distributionItemRepository.saveAll(byId.values());

        distribution.setStatus(DistributionStatus.COMPLETED);
        distribution.setCompletedBy(actor);
        distribution.setCompletedAt(LocalDateTime.now());
        appendNotes(distribution, request != null ? request.getNotes() : null);
        distributionRepository.save(distribution);

        long totalDistributed = quantities.values().stream().mapToLong(Integer::longValue).sum();
        auditService.record(AuditAction.COMPLETE_DISTRIBUTION, "DistributionRequest", id,
                "Completed distribution " + distribution.getRequestReference() + " to "
                        + beneficiary.getBeneficiaryName(),
                Map.of("status", DistributionStatus.APPROVED),
                Map.of("status", DistributionStatus.COMPLETED,
                        "itemLines", items.size(),
                        "totalDistributedQuantity", totalDistributed,
                        "duplicateOverrideApplied", overrideRequested));

        log.info("Completed distribution {} to beneficiary {}: {} lines, {} units deducted from stock",
                distribution.getRequestReference(), beneficiary.getBeneficiaryCode(), items.size(),
                totalDistributed);
        return mapper.toResponse(distribution, items);
    }

    // ── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DistributionResponse get(Long id) {
        return toResponse(requireRequest(id));
    }

    @Transactional(readOnly = true)
    public DistributionResponse getByReference(String reference) {
        DistributionRequest distribution = distributionRepository.findByRequestReferenceIgnoreCase(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution request", reference));
        return toResponse(distribution);
    }

    @Transactional(readOnly = true)
    public Page<DistributionResponse> search(String search,
                                             DistributionStatus status,
                                             PriorityLevel priority,
                                             Long beneficiaryId,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "requestDate");
        return distributionRepository
                .findAll(DistributionSpecifications.build(search, status, priority, beneficiaryId,
                        startDate, endDate), safe)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DistributionReportResponse getReport(Long id) {
        DistributionRequest distribution = requireRequest(id);
        return mapper.toReport(distribution,
                distributionItemRepository.findByDistributionRequestId(id),
                overrideRepository.findByDistributionRequestIdOrderByCreatedAtDesc(id));
    }

    @Transactional(readOnly = true)
    public DuplicateDistributionResponse checkDuplicate(Long beneficiaryId, Long inventoryItemId) {
        return duplicateChecker.check(beneficiaryService.requireBeneficiary(beneficiaryId),
                inventoryItemService.requireItem(inventoryItemId));
    }

    @Transactional(readOnly = true)
    public BeneficiaryStatisticsResponse getBeneficiaryStatistics(Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryService.requireBeneficiary(beneficiaryId);
        Page<DistributionResponse> recent = search(null, DistributionStatus.COMPLETED, null, beneficiaryId,
                null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "requestDate")));

        return BeneficiaryStatisticsResponse.builder()
                .beneficiaryId(beneficiary.getId())
                .beneficiaryCode(beneficiary.getBeneficiaryCode())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .priorityLevel(beneficiary.getPriorityLevel())
                .familySize(beneficiary.getFamilySize())
                .totalDistributions(distributionRepository.countByBeneficiaryIdAndStatus(beneficiaryId,
                        DistributionStatus.COMPLETED))
                .totalItemsReceived(distributionItemRepository.countItemLinesByBeneficiary(beneficiaryId))
                .totalQuantityReceived(
                        distributionItemRepository.sumDistributedQuantityByBeneficiary(beneficiaryId))
                .lastDistributionDate(distributionRepository.findLastCompletedAt(beneficiaryId))
                .pendingRequests(distributionRepository.countByBeneficiaryIdAndStatus(beneficiaryId,
                        DistributionStatus.PENDING))
                .approvedRequests(distributionRepository.countByBeneficiaryIdAndStatus(beneficiaryId,
                        DistributionStatus.APPROVED))
                .recentDistributions(recent.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public DistributionRequest requireRequest(Long id) {
        return distributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution request", id));
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private List<DistributionItem> createItems(DistributionRequest distribution,
                                               List<DistributionItemRequest> requestedItems) {
        List<DistributionItem> items = new ArrayList<>();
        Set<Long> seenItemIds = new HashSet<>();

        for (DistributionItemRequest itemRequest : requestedItems) {
            InventoryItem inventoryItem = inventoryItemService.requireItem(itemRequest.getInventoryItemId());
            if (inventoryItem.isArchived()) {
                throw new BadRequestException("Inventory item " + inventoryItem.getItemCode()
                        + " is archived and cannot be distributed", "ITEM_ARCHIVED");
            }
            if (!seenItemIds.add(inventoryItem.getId())) {
                throw new BadRequestException("Item " + inventoryItem.getItemName()
                        + " appears more than once in the request; combine the quantities instead",
                        "DUPLICATE_REQUEST_LINE");
            }
            items.add(distributionItemRepository.save(DistributionItem.builder()
                    .distributionRequest(distribution)
                    .inventoryItem(inventoryItem)
                    .requestedQuantity(itemRequest.getRequestedQuantity())
                    .allocatedQuantity(0)
                    .distributedQuantity(0)
                    .notes(trimToNull(itemRequest.getNotes()))
                    .build()));
        }
        return items;
    }

    /**
     * Works out how much of each line actually leaves the store, defaulting to the allocated
     * quantity when the caller does not say otherwise.
     */
    private Map<Long, Integer> resolveDistributedQuantities(DistributionRequest distribution,
                                                            Map<Long, DistributionItem> byId,
                                                            CompleteDistributionRequest request) {
        Map<Long, Integer> supplied = new HashMap<>();
        if (request != null && request.getItems() != null) {
            for (CompleteDistributionRequest.CompletionLine line : request.getItems()) {
                requireLine(byId, line.getDistributionItemId(), distribution);
                supplied.put(line.getDistributionItemId(), line.getDistributedQuantity());
            }
        }

        Map<Long, Integer> resolved = new LinkedHashMap<>();
        for (DistributionItem item : byId.values()) {
            int allocated = item.getAllocatedQuantity() == null ? 0 : item.getAllocatedQuantity();
            int quantity = supplied.getOrDefault(item.getId(), allocated);

            if (quantity <= 0) {
                throw new BadRequestException("Nothing is allocated for "
                        + item.getInventoryItem().getItemName() + "; allocate stock before completing "
                        + distribution.getRequestReference(), "ALLOCATION_REQUIRED");
            }
            if (quantity > allocated) {
                throw new BadRequestException("Cannot distribute " + quantity + " of "
                        + item.getInventoryItem().getItemName() + " when only " + allocated
                        + " is allocated", "EXCEEDS_ALLOCATION");
            }
            resolved.put(item.getId(), quantity);
        }
        return resolved;
    }

    private void recordOverride(DistributionRequest distribution,
                                Beneficiary beneficiary,
                                InventoryItem inventoryItem,
                                DuplicateDistributionResponse duplicate,
                                String reason,
                                User actor) {
        DistributionOverride override = overrideRepository.save(DistributionOverride.builder()
                .distributionRequest(distribution)
                .beneficiary(beneficiary)
                .inventoryItem(inventoryItem)
                .previousDistributionId(duplicate.getPreviousDistributionId())
                .daysSinceLastDistribution(duplicate.getDaysSinceLastDistribution())
                .overrideReason(reason)
                .overriddenBy(actor)
                .createdAt(LocalDateTime.now())
                .build());

        auditService.record(AuditAction.OVERRIDE_DUPLICATE, "DistributionRequest", distribution.getId(),
                "Duplicate-distribution warning overridden for " + inventoryItem.getItemName()
                        + " to " + beneficiary.getBeneficiaryName(),
                null, Map.of(
                        "overrideId", override.getId(),
                        "inventoryItemId", inventoryItem.getId(),
                        "previousDistributionId", String.valueOf(duplicate.getPreviousDistributionId()),
                        "daysSinceLastDistribution", String.valueOf(duplicate.getDaysSinceLastDistribution()),
                        "reason", reason));

        log.warn("ADMIN {} overrode duplicate-distribution warning on {} for item {} (last received {} day(s) ago)",
                actor.getEmail(), distribution.getRequestReference(), inventoryItem.getItemCode(),
                duplicate.getDaysSinceLastDistribution());
    }

    private Map<Long, DistributionItem> itemsById(Long distributionId) {
        List<DistributionItem> items = distributionItemRepository.findByDistributionRequestId(distributionId);
        if (items.isEmpty()) {
            throw new InvalidDistributionException("Distribution request " + distributionId
                    + " has no items", "NO_DISTRIBUTION_ITEMS");
        }
        return items.stream().collect(Collectors.toMap(DistributionItem::getId, Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
    }

    private DistributionItem requireLine(Map<Long, DistributionItem> byId, Long lineId,
                                         DistributionRequest distribution) {
        DistributionItem item = byId.get(lineId);
        if (item == null) {
            throw new BadRequestException("Distribution item " + lineId + " does not belong to "
                    + distribution.getRequestReference());
        }
        return item;
    }

    private void requireStatus(DistributionRequest distribution, DistributionStatus expected, String action) {
        if (distribution.getStatus() != expected) {
            throw new InvalidDistributionException("Only an " + expected + " distribution can be " + action
                    + "; " + distribution.getRequestReference() + " is " + distribution.getStatus());
        }
    }

    private void assertBeneficiaryActive(Beneficiary beneficiary) {
        if (beneficiary.getStatus() != RecordStatus.ACTIVE) {
            throw new BadRequestException("Beneficiary " + beneficiary.getBeneficiaryCode()
                    + " is inactive and cannot receive aid", "BENEFICIARY_INACTIVE");
        }
    }

    private DistributionResponse toResponse(DistributionRequest distribution) {
        return mapper.toResponse(distribution,
                distributionItemRepository.findByDistributionRequestId(distribution.getId()));
    }

    private static void appendNotes(DistributionRequest distribution, String extra) {
        String trimmed = trimToNull(extra);
        if (trimmed == null) {
            return;
        }
        String existing = distribution.getNotes();
        String combined = existing == null || existing.isBlank() ? trimmed : existing + "\n" + trimmed;
        distribution.setNotes(combined.length() > 1000 ? combined.substring(0, 1000) : combined);
    }

    private static Map<String, Object> snapshot(DistributionRequest distribution) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("requestReference", distribution.getRequestReference());
        values.put("requestDate", distribution.getRequestDate());
        values.put("priority", distribution.getPriority());
        values.put("reason", distribution.getReason());
        values.put("status", distribution.getStatus());
        return values;
    }

    private static Map<String, Object> allocationSnapshot(Iterable<DistributionItem> items) {
        Map<String, Object> values = new LinkedHashMap<>();
        items.forEach(item -> values.put("item-" + item.getId(), item.getAllocatedQuantity()));
        return values;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
