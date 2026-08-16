package com.charitymanagement.api.charitymanagementback.inventory.dto.request;

import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdjustStockRequest {

    /**
     * Signed movement: positive adds stock, negative removes it. Zero is rejected — an adjustment
     * that changes nothing should not create a ledger row.
     */
    @NotNull(message = "Adjustment quantity is required")
    private Integer adjustment;

    /** MANUAL_ADJUSTMENT (default) or CORRECTION. */
    private TransactionType transactionType;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
