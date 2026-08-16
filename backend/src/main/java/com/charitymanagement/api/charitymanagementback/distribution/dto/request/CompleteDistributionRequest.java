package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Confirms that aid physically left the store. This is the only request in the system that
 * decreases inventory.
 */
@Data
public class CompleteDistributionRequest {

    /** Optional. Lines omitted here are distributed at their allocated quantity. */
    @Valid
    private List<CompletionLine> items;

    /**
     * Set by an ADMIN to proceed despite a duplicate-distribution warning. Requires
     * {@link #overrideReason}; any other role is refused.
     */
    private boolean overrideDuplicates;

    @Size(max = 1000, message = "Override reason must not exceed 1000 characters")
    private String overrideReason;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Data
    public static class CompletionLine {

        @NotNull(message = "Distribution item id is required")
        private Long distributionItemId;

        @NotNull(message = "Distributed quantity is required")
        @Positive(message = "Distributed quantity must be greater than zero")
        private Integer distributedQuantity;
    }
}
