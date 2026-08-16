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
public class DistributionReportSummaryResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private long completedDistributions;
    private long totalItemLines;
    private long totalQuantityDistributed;

    private List<ReportBreakdownEntry> statusBreakdown;
    private List<ReportBreakdownEntry> priorityBreakdown;
    private List<ReportBreakdownEntry> beneficiaryBreakdown;
    private List<ReportBreakdownEntry> itemBreakdown;
    private List<TrendPointResponse> monthlyTrend;

    private LocalDateTime generatedAt;
}
