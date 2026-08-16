package com.charitymanagement.api.charitymanagementback.inventory.dto.request;

import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInventoryRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 200, message = "Item name must not exceed 200 characters")
    private String itemName;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /** Opening stock. Further changes must go through donations, distributions or adjust-stock. */
    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "Unit is required")
    private UnitOfMeasure unit;

    @NotNull(message = "Minimum stock level is required")
    @PositiveOrZero(message = "Minimum stock level cannot be negative")
    private Integer minimumStockLevel;

    private LocalDate expiryDate;
}
