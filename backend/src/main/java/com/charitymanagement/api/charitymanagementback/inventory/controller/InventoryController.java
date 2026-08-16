package com.charitymanagement.api.charitymanagementback.inventory.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.AdjustStockRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.CreateInventoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.request.UpdateInventoryRequest;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryTransactionResponse;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ExpiryFilter;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryItemService;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryTransactionQueryService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock items, quantities, expiry and the stock ledger")
public class InventoryController {

    private final InventoryItemService itemService;
    private final InventoryTransactionQueryService transactionQueryService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Add an inventory item",
            description = "Item codes (INV-000001) are generated. Any opening quantity is written to the ledger.")
    public ResponseEntity<ApiResponse<InventoryResponse>> create(@Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(itemService.create(request), "Inventory item created successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Search inventory",
            description = "Filter by text, category, stock status and expiry window; sortable and paged.")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) ExpiryFilter expiryStatus,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InventoryResponse> page =
                itemService.search(search, categoryId, status, expiryStatus, includeArchived, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Items at or below their minimum stock level")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> lowStock(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InventoryResponse> page = itemService.findByStatus(InventoryStatus.LOW_STOCK, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Items with zero quantity")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> outOfStock(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InventoryResponse> page = itemService.findByStatus(InventoryStatus.OUT_OF_STOCK, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/expired")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Items whose expiry date has passed",
            description = "Expired rows are retained for audit purposes and are never deleted automatically.")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> expired(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InventoryResponse> page = itemService.findExpired(pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Items expiring within the configured horizon",
            description = "Defaults to charity.inventory.expiring-soon-days; override with ?days=")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> expiringSoon(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success(itemService.findExpiringSoon(days)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Get one inventory item")
    public ResponseEntity<ApiResponse<InventoryResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Update an inventory item",
            description = "Quantity is not editable here — use adjust-stock so the change is recorded.")
    public ResponseEntity<ApiResponse<InventoryResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(itemService.update(id, request), "Inventory item updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Archive an inventory item",
            description = "Soft delete. Donation, distribution and ledger history reference this row, so it is "
                    + "archived rather than removed. Archived items are excluded from listings and refused by "
                    + "every stock movement.")
    public ResponseEntity<ApiResponse<InventoryResponse>> archive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.archive(id),
                "Inventory item archived; historical records were preserved"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Restore an archived inventory item")
    public ResponseEntity<ApiResponse<InventoryResponse>> restore(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(itemService.restore(id), "Inventory item restored successfully"));
    }

    @PostMapping("/{id}/adjust-stock")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Adjust stock manually",
            description = "Signed adjustment. Refused with 409 INSUFFICIENT_STOCK if it would drive stock below "
                    + "zero. Always writes an inventory transaction.")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(@PathVariable Long id,
                                                                      @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(itemService.adjustStock(id, request), "Stock adjusted successfully"));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Stock ledger for one item")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> transactions(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        Page<InventoryTransactionResponse> page = transactionQueryService.findForItem(id, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }
}
