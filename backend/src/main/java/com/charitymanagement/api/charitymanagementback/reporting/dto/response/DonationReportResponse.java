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
public class DonationReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private long totalDonations;
    private long totalItemLines;
    private long totalQuantityDonated;

    /** Per-donor contribution totals, busiest donor first. */
    private List<ReportBreakdownEntry> donorContributions;
    /** Donations grouped by calendar month across the selected range. */
    private List<TrendPointResponse> monthlyTrend;

    private LocalDateTime generatedAt;
}
