package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryTransactionResponse;
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
public class StockMovementReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private long totalMovements;
    private long totalQuantityIn;
    private long totalQuantityOut;

    /** Movement counts and absolute quantities per transaction type. */
    private List<ReportBreakdownEntry> typeBreakdown;

    private List<InventoryTransactionResponse> movements;
    private PaginationMeta pagination;

    private LocalDateTime generatedAt;
}
