package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Every figure here is counted from charity_db at request time; none of it is cached or seeded. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryResponse {

    private long totalInventoryItems;
    private long totalStockUnits;
    private long lowStockItems;
    private long outOfStockItems;
    private long expiredItems;
    private long expiringSoonItems;

    private long totalCategories;
    private long totalDonors;
    private long totalBeneficiaries;
    private long totalVolunteers;

    private long donationsThisMonth;
    private long distributionsThisMonth;
    private long pendingDistributionRequests;
    private long approvedDistributionRequests;
    private long pendingVolunteerTasks;

    private LocalDateTime generatedAt;
}
