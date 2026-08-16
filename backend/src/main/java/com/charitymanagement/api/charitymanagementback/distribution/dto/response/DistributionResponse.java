package com.charitymanagement.api.charitymanagementback.distribution.dto.response;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DistributionResponse {

    private Long id;
    private String requestReference;
    private Long beneficiaryId;
    private String beneficiaryCode;
    private String beneficiaryName;
    private LocalDate requestDate;
    private PriorityLevel priority;
    private String reason;
    private String notes;
    private DistributionStatus status;

    private UserSummary requestedBy;
    private UserSummary approvedBy;
    private LocalDateTime approvedAt;
    private UserSummary rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private UserSummary cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private UserSummary completedBy;
    private LocalDateTime completedAt;

    private int totalItems;
    private long totalRequestedQuantity;
    private long totalAllocatedQuantity;
    private long totalDistributedQuantity;
    private List<DistributionItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
