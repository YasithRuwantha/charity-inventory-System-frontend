package com.charitymanagement.api.charitymanagementback.inventory.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.exception.DuplicateResourceException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.CreateCategoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.UpdateCategoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.CategoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryCategory;
import com.charitymanagement.api.charitymanagementback.inventory.mapper.InventoryMapper;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryCategoryRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryCategoryService {

    private static final Set<String> SORTABLE = Set.of("name", "status", "createdAt", "updatedAt", "id");

    private final InventoryCategoryRepository categoryRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryMapper mapper;
    private final AuditService auditService;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists");
        }
        InventoryCategory category = categoryRepository.save(InventoryCategory.builder()
                .name(name)
                .description(trimToNull(request.getDescription()))
                .status(request.getStatus() != null ? request.getStatus() : RecordStatus.ACTIVE)
                .build());

        auditService.record(AuditAction.CREATE_CATEGORY, "InventoryCategory", category.getId(),
                "Created category " + category.getName(), null, snapshot(category));
        return mapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        InventoryCategory category = requireCategory(id);
        Map<String, Object> before = snapshot(category);

        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));
        if (request.getStatus() != null) {
            applyStatusChange(category, request.getStatus());
        }
        categoryRepository.save(category);

        auditService.record(AuditAction.UPDATE_CATEGORY, "InventoryCategory", id,
                "Updated category " + category.getName(), before, snapshot(category));
        return mapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse changeStatus(Long id, RecordStatus status) {
        InventoryCategory category = requireCategory(id);
        Map<String, Object> before = snapshot(category);

        applyStatusChange(category, status);
        categoryRepository.save(category);

        auditService.record(AuditAction.CHANGE_CATEGORY_STATUS, "InventoryCategory", id,
                "Category " + category.getName() + " set to " + status, before, snapshot(category));
        return mapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return mapper.toResponse(requireCategory(id));
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(String search, RecordStatus status, Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "name");
        String pattern = search == null || search.isBlank()
                ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return categoryRepository.search(pattern, status, safe).map(mapper::toResponse);
    }

    /** Shared lookup used by the inventory item service. */
    @Transactional(readOnly = true)
    public InventoryCategory requireCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory category", id));
    }

    /** Deactivating a category is blocked while active items still point at it. */
    private void applyStatusChange(InventoryCategory category, RecordStatus status) {
        if (status == RecordStatus.INACTIVE && category.getStatus() != RecordStatus.INACTIVE
                && itemRepository.existsByCategoryIdAndArchivedFalse(category.getId())) {
            throw new BadRequestException(
                    "Category '" + category.getName() + "' still has active inventory items and cannot be deactivated",
                    "CATEGORY_IN_USE");
        }
        category.setStatus(status);
    }

    private static Map<String, Object> snapshot(InventoryCategory category) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", category.getName());
        values.put("description", category.getDescription());
        values.put("status", category.getStatus());
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
