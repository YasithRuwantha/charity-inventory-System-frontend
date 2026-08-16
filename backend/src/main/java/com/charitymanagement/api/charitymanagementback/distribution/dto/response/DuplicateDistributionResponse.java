package com.charitymanagement.api.charitymanagementback.distribution.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outcome of a duplicate check for one beneficiary/item pair. Returned on its own by
 * {@code GET /api/distributions/check-duplicate}, and embedded in the 409 body when a completion
 * is blocked.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DuplicateDistributionResponse {

    private boolean duplicateWarning;
    private String message;

    private Long beneficiaryId;
    private String beneficiaryName;
    private Long inventoryItemId;
    private String itemName;

    private Long previousDistributionId;
    private String previousDistributionReference;
    private LocalDateTime lastDistributionDate;
    private Integer daysSinceLastDistribution;
    private Integer quantityPreviouslyReceived;

    /** The configured window, so a caller can explain the rule without reading the config. */
    private int duplicateWindowDays;
}
