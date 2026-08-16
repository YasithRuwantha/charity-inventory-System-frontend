package com.charitymanagement.api.charitymanagementback.inventory.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.dto.request.StatusUpdateRequest;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.CreateCategoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.UpdateCategoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.CategoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Inventory categories", description = "Classification of inventory items")
public class InventoryCategoryController {

    private final InventoryCategoryService categoryService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Create a category",
            description = "ADMIN or INVENTORY_STAFF. Category names are unique, case-insensitively.")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(request), "Category created successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "List categories", description = "Supports search, status filter, sorting and paging.")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CategoryResponse> page = categoryService.search(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Get one category")
    public ResponseEntity<ApiResponse<CategoryResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(categoryService.update(id, request), "Category updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Activate or deactivate a category",
            description = "Deactivation is refused while active inventory items still reference the category.")
    public ResponseEntity<ApiResponse<CategoryResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.changeStatus(id, request.getStatus()),
                "Category status updated successfully"));
    }
}
