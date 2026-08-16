package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

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
public class BeneficiaryReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private long totalBeneficiaries;
    private long activeBeneficiaries;
    private long inactiveBeneficiaries;

    private List<ReportBreakdownEntry> statusBreakdown;
    private List<ReportBreakdownEntry> priorityBreakdown;
    /** Aid actually received per beneficiary over the selected range. */
    private List<ReportBreakdownEntry> distributionBreakdown;

    private LocalDateTime generatedAt;
}
