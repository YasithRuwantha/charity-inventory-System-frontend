package com.charitymanagement.api.charitymanagementback.donation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DonationItemRequest {

    @NotNull(message = "Inventory item is required")
    private Long inventoryItemId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    /** Expiry of this donated batch, if the donor supplied one. */
    private LocalDate expiryDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
