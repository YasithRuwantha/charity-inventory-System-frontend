package com.charitymanagement.api.charitymanagementback.inventory.dto.response;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponse {

    private Long id;
    private String itemCode;
    private String itemName;
    private Long categoryId;
    private String categoryName;
    private String description;
    private Integer quantity;
    private UnitOfMeasure unit;
    private Integer minimumStockLevel;
    private LocalDate expiryDate;
    private InventoryStatus status;
    private boolean expired;
    /** Null when the item has no expiry date; negative once the date has passed. */
    private Long daysUntilExpiry;
    private boolean archived;
    private UserSummary createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
