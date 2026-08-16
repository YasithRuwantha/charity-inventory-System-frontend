package com.charitymanagement.api.charitymanagementback.beneficiary.dto.response;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BeneficiaryStatisticsResponse {

    private Long beneficiaryId;
    private String beneficiaryCode;
    private String beneficiaryName;
    private PriorityLevel priorityLevel;
    private Integer familySize;

    private long totalDistributions;
    private long totalItemsReceived;
    private long totalQuantityReceived;
    private LocalDateTime lastDistributionDate;
    private long pendingRequests;
    private long approvedRequests;

    /** Ten most recent completed distributions, newest first. */
    private List<DistributionResponse> recentDistributions;
}
