package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

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
public class InventoryReportResponse {

    private long totalItems;
    private long totalQuantity;
    private long lowStockItems;
    private long outOfStockItems;
    private long expiredItems;
    private long expiringSoonItems;
    private int expiringSoonHorizonDays;

    private List<ReportBreakdownEntry> categoryBreakdown;
    private List<ReportBreakdownEntry> statusBreakdown;

    private LocalDateTime generatedAt;
}
