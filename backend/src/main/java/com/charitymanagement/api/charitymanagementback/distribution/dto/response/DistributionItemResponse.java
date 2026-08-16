package com.charitymanagement.api.charitymanagementback.distribution.dto.response;

import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DistributionItemResponse {

    private Long id;
    private Long inventoryItemId;
    private String itemCode;
    private String itemName;
    private String categoryName;
    private UnitOfMeasure unit;
    private Integer requestedQuantity;
    private Integer allocatedQuantity;
    private Integer distributedQuantity;
    private String notes;

    /** Live stock figures, so a reviewer can see what is actually available right now. */
    private Integer availableQuantity;
    private InventoryStatus inventoryStatus;
    private LocalDate expiryDate;
    private boolean expired;
}
