package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.List;

/**
 * Earmarks stock against the request. Allocation is a planning step only — it never moves
 * inventory, which is why an allocated quantity can still fail at completion time if the stock
 * was consumed elsewhere in between.
 */
@Data
public class AllocateDistributionRequest {

    @NotEmpty(message = "At least one allocation line is required")
    @Valid
    private List<AllocationLine> items;

    @Data
    public static class AllocationLine {

        @NotNull(message = "Distribution item id is required")
        private Long distributionItemId;

        @NotNull(message = "Allocated quantity is required")
        @PositiveOrZero(message = "Allocated quantity cannot be negative")
        private Integer allocatedQuantity;
    }
}
