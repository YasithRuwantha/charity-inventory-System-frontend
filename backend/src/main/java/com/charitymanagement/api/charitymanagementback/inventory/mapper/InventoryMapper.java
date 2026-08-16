package com.charitymanagement.api.charitymanagementback.inventory.mapper;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.CategoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryTransactionResponse;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryCategory;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryTransaction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Entity → DTO conversion. Must be invoked inside an active transaction: it walks the lazy
 * {@code category} and {@code createdBy} associations.
 */
@Component
public class InventoryMapper {

    public CategoryResponse toResponse(InventoryCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public InventoryResponse toResponse(InventoryItem item) {
        Long daysUntilExpiry = item.getExpiryDate() == null ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), item.getExpiryDate());

        return InventoryResponse.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .minimumStockLevel(item.getMinimumStockLevel())
                .expiryDate(item.getExpiryDate())
                .status(item.getStatus())
                .expired(item.isExpired())
                .daysUntilExpiry(daysUntilExpiry)
                .archived(item.isArchived())
                .createdBy(UserSummary.from(item.getCreatedBy()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public InventoryTransactionResponse toResponse(InventoryTransaction transaction) {
        InventoryItem item = transaction.getInventoryItem();
        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .inventoryItemId(item != null ? item.getId() : null)
                .itemCode(item != null ? item.getItemCode() : null)
                .itemName(item != null ? item.getItemName() : null)
                .transactionType(transaction.getTransactionType())
                .quantity(transaction.getQuantity())
                .quantityBefore(transaction.getQuantityBefore())
                .quantityAfter(transaction.getQuantityAfter())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .notes(transaction.getNotes())
                .performedBy(UserSummary.from(transaction.getPerformedBy()))
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
