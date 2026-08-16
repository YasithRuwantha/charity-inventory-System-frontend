package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DistributionItemRequest {

    @NotNull(message = "Inventory item id is required")
    private Long inventoryItemId;

    @NotNull(message = "Requested quantity is required")
    @Positive(message = "Requested quantity must be greater than zero")
    private Integer requestedQuantity;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
