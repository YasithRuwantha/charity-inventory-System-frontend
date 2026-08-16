package com.charitymanagement.api.charitymanagementback.inventory.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.config.CharityProperties;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.exception.DuplicateResourceException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.reference.ReferenceGenerator;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.AdjustStockRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.CreateInventoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.UpdateInventoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryCategory;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryTransaction;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ExpiryFilter;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.inventory.mapper.InventoryMapper;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemSpecifications;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private static final Logger log = LoggerFactory.getLogger(InventoryItemService.class);

    private static final Set<String> SORTABLE = Set.of(
            "id", "itemCode", "itemName", "quantity", "minimumStockLevel", "expiryDate", "status",
            "createdAt", "updatedAt");

    private final InventoryItemRepository itemRepository;
    private final InventoryCategoryService categoryService;
    private final InventoryStockService stockService;
    private final InventoryMapper mapper;
    private final ReferenceGenerator referenceGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final CharityProperties properties;

    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {
        InventoryCategory category = categoryService.requireCategory(request.getCategoryId());
        if (category.getStatus() != RecordStatus.ACTIVE) {
            throw new BadRequestException("Category '" + category.getName() + "' is inactive", "CATEGORY_INACTIVE");
        }
        String itemName = request.getItemName().trim();
        if (itemRepository.existsByItemNameIgnoreCaseAndCategoryIdAndArchivedFalse(itemName, category.getId())) {
            throw new DuplicateResourceException(
                    "An item named '" + itemName + "' already exists in category " + category.getName());
        }
        User actor = currentUserProvider.requireManagedUser();

        InventoryItem item = InventoryItem.builder()
                .itemCode(referenceGenerator.next(ReferenceGenerator.INVENTORY_ITEM))
                .itemName(itemName)
                .category(category)
                .description(trimToNull(request.getDescription()))
                // Opening stock is applied below as a ledger movement rather than written directly.
                .quantity(0)
                .unit(request.getUnit())
                .minimumStockLevel(request.getMinimumStockLevel())
                .expiryDate(request.getExpiryDate())
                .createdBy(actor)
                .build();
        item.recalculateStatus();
        item = itemRepository.save(item);

        if (request.getQuantity() != null && request.getQuantity() > 0) {
            stockService.increase(item.getId(), request.getQuantity(), TransactionType.MANUAL_ADJUSTMENT,
                    ReferenceType.SYSTEM, null, "Opening stock recorded when the item was created", actor, null);
        }

        auditService.record(AuditAction.CREATE_INVENTORY, "InventoryItem", item.getId(),
                "Created inventory item " + item.getItemCode() + " (" + item.getItemName() + ")",
                null, snapshot(item));
        log.info("Created inventory item {} in category {}", item.getItemCode(), category.getName());
        return mapper.toResponse(item);
    }

    @Transactional
    public InventoryResponse update(Long id, UpdateInventoryRequest request) {
        InventoryItem item = requireItem(id);
        assertNotArchived(item);
        Map<String, Object> before = snapshot(item);

        InventoryCategory category = categoryService.requireCategory(request.getCategoryId());
        String itemName = request.getItemName().trim();
        boolean nameOrCategoryChanged = !itemName.equalsIgnoreCase(item.getItemName())
                || !category.getId().equals(item.getCategory().getId());
        if (nameOrCategoryChanged
                && itemRepository.existsByItemNameIgnoreCaseAndCategoryIdAndArchivedFalse(itemName, category.getId())) {
            throw new DuplicateResourceException(
                    "An item named '" + itemName + "' already exists in category " + category.getName());
        }

        item.setItemName(itemName);
        item.setCategory(category);
        item.setDescription(trimToNull(request.getDescription()));
        item.setUnit(request.getUnit());
        item.setMinimumStockLevel(request.getMinimumStockLevel());
        item.setExpiryDate(request.getExpiryDate());
        item.recalculateStatus();
        itemRepository.save(item);

        auditService.record(AuditAction.UPDATE_INVENTORY, "InventoryItem", id,
                "Updated inventory item " + item.getItemCode(), before, snapshot(item));
        return mapper.toResponse(item);
    }

    /**
     * Soft delete. Inventory rows are referenced by donation, distribution and ledger history, so
     * they are archived rather than removed — archived items are hidden from the default listing
     * and refused by every stock movement.
     */
    @Transactional
    public InventoryResponse archive(Long id) {
        InventoryItem item = requireItem(id);
        if (item.isArchived()) {
            throw new BadRequestException("Inventory item " + item.getItemCode() + " is already archived",
                    "ITEM_ALREADY_ARCHIVED");
        }
        Map<String, Object> before = snapshot(item);
        item.setArchived(true);
        itemRepository.save(item);

        auditService.record(AuditAction.ARCHIVE_INVENTORY, "InventoryItem", id,
                "Archived inventory item " + item.getItemCode(), before, snapshot(item));
        log.info("Archived inventory item {}", item.getItemCode());
        return mapper.toResponse(item);
    }

    @Transactional
    public InventoryResponse restore(Long id) {
        InventoryItem item = requireItem(id);
        if (!item.isArchived()) {
            throw new BadRequestException("Inventory item " + item.getItemCode() + " is not archived",
                    "ITEM_NOT_ARCHIVED");
        }
        item.setArchived(false);
        item.recalculateStatus();
        itemRepository.save(item);

        auditService.record(AuditAction.UPDATE_INVENTORY, "InventoryItem", id,
                "Restored inventory item " + item.getItemCode());
        return mapper.toResponse(item);
    }

    @Transactional
    public InventoryResponse adjustStock(Long id, AdjustStockRequest request) {
        TransactionType type = request.getTransactionType() != null
                ? request.getTransactionType() : TransactionType.MANUAL_ADJUSTMENT;
        if (type != TransactionType.MANUAL_ADJUSTMENT && type != TransactionType.CORRECTION) {
            throw new BadRequestException(
                    "Manual adjustments must use MANUAL_ADJUSTMENT or CORRECTION; donations and distributions "
                            + "create their own ledger entries");
        }
        User actor = currentUserProvider.requireManagedUser();
        InventoryTransaction transaction = stockService.adjust(id, request.getAdjustment(), type,
                trimToNull(request.getNotes()), actor);

        auditService.record(AuditAction.ADJUST_STOCK, "InventoryItem", id,
                "Adjusted stock of " + transaction.getInventoryItem().getItemCode() + " by "
                        + request.getAdjustment(),
                Map.of("quantity", transaction.getQuantityBefore()),
                Map.of("quantity", transaction.getQuantityAfter(), "notes", String.valueOf(request.getNotes())));

        return mapper.toResponse(transaction.getInventoryItem());
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Long id) {
        return mapper.toResponse(requireItem(id));
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> search(String search,
                                          Long categoryId,
                                          InventoryStatus status,
                                          ExpiryFilter expiryFilter,
                                          boolean includeArchived,
                                          Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "createdAt");
        Specification<InventoryItem> specification =
                InventoryItemSpecifications.build(search, categoryId, status, expiryFilter, includeArchived);
        return itemRepository.findAll(specification, safe).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> findByStatus(InventoryStatus status, Pageable pageable) {
        return search(null, null, status, null, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> findExpired(Pageable pageable) {
        return search(null, null, null, ExpiryFilter.EXPIRED, false, pageable);
    }

    /** Items whose expiry falls between today and today + {@code days} (default from configuration). */
    @Transactional(readOnly = true)
    public List<InventoryResponse> findExpiringSoon(Integer days) {
        int horizon = days != null && days > 0 ? days : properties.getInventory().getExpiringSoonDays();
        LocalDate today = LocalDate.now();
        return itemRepository.findExpiringBetween(today, today.plusDays(horizon)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryItem requireItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", id));
    }

    /**
     * Used by the bulk donation import, which identifies items by their human-readable name.
     * Item names are unique within a category but may repeat across categories, so every match is
     * returned and the caller disambiguates.
     */
    @Transactional(readOnly = true)
    public List<InventoryItem> findActiveByName(String itemName) {
        return itemRepository.findByItemNameIgnoreCaseAndArchivedFalse(itemName.trim());
    }

    private void assertNotArchived(InventoryItem item) {
        if (item.isArchived()) {
            throw new BadRequestException("Inventory item " + item.getItemCode() + " is archived",
                    "ITEM_ARCHIVED");
        }
    }

    private static Map<String, Object> snapshot(InventoryItem item) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("itemCode", item.getItemCode());
        values.put("itemName", item.getItemName());
        values.put("categoryId", item.getCategory() != null ? item.getCategory().getId() : null);
        values.put("quantity", item.getQuantity());
        values.put("unit", item.getUnit());
        values.put("minimumStockLevel", item.getMinimumStockLevel());
        values.put("expiryDate", item.getExpiryDate());
        values.put("status", item.getStatus());
        values.put("archived", item.isArchived());
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
