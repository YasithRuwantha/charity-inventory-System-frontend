package com.charitymanagement.api.charitymanagementback.reporting.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DashboardAlertResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DashboardSummaryResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.RecentActivityResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.ReportBreakdownEntry;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.TrendPointResponse;
import com.charitymanagement.api.charitymanagementback.reporting.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only aggregates for a future dashboard UI. Every figure is queried from charity_db on each
 * call — nothing here is cached, seeded or hard-coded.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Live summary counters, trends and operational alerts")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Dashboard summary",
            description = "Inventory, donor, beneficiary, volunteer and distribution counters for the current "
                    + "month, counted live from the database.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.summary()));
    }

    @GetMapping("/donation-trend")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Monthly donation trend",
            description = "Donation count and donated quantity per month. Accepts ?period=3months, 6months "
                    + "(default) or 12months; months with no activity come back as zeroes.")
    public ResponseEntity<ApiResponse<List<TrendPointResponse>>> donationTrend(
            @RequestParam(required = false, defaultValue = "6months") String period) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.donationTrend(period)));
    }

    @GetMapping("/distribution-trend")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Monthly distribution trend",
            description = "Completed distributions and distributed quantity per month. Same ?period= values as "
                    + "the donation trend.")
    public ResponseEntity<ApiResponse<List<TrendPointResponse>>> distributionTrend(
            @RequestParam(required = false, defaultValue = "6months") String period) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.distributionTrend(period)));
    }

    @GetMapping("/inventory-by-category")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Stock split by category",
            description = "Item count and total units held per inventory category.")
    public ResponseEntity<ApiResponse<List<ReportBreakdownEntry>>> inventoryByCategory() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.inventoryByCategory()));
    }

    @GetMapping("/beneficiary-priority")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Beneficiaries by priority level",
            description = "How many registered beneficiaries sit at HIGH, MEDIUM and LOW priority.")
    public ResponseEntity<ApiResponse<List<ReportBreakdownEntry>>> beneficiaryPriority() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.beneficiaryPriority()));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Recent activity feed",
            description = "The twenty most recent audited actions, newest first.")
    public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> recentActivity() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.recentActivity()));
    }

    @GetMapping("/alerts")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Operational alerts",
            description = "Expired and out-of-stock items are CRITICAL because they block distributions; "
                    + "low stock, expiring stock and overdue tasks are advisory. High-priority pending "
                    + "requests are listed individually.")
    public ResponseEntity<ApiResponse<List<DashboardAlertResponse>>> alerts() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.alerts()));
    }
}
