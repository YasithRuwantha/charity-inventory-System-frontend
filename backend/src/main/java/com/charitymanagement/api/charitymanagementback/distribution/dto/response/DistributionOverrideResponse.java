package com.charitymanagement.api.charitymanagementback.distribution.dto.response;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DistributionOverrideResponse {

    private Long id;
    private Long distributionRequestId;
    private String distributionReference;
    private Long beneficiaryId;
    private String beneficiaryName;
    private Long inventoryItemId;
    private String itemName;
    private Long previousDistributionId;
    private Integer daysSinceLastDistribution;
    private String overrideReason;
    private UserSummary overriddenBy;
    private LocalDateTime createdAt;
}
