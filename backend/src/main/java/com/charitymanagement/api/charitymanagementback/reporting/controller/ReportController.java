package com.charitymanagement.api.charitymanagementback.reporting.controller;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.BeneficiaryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DistributionReportSummaryResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DonationReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.ExpiryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.InventoryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.StockMovementReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.VolunteerReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.service.CsvExportService;
import com.charitymanagement.api.charitymanagementback.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregated reporting across every module, with CSV export")
public class ReportController {

    private final ReportService reportService;
    private final CsvExportService csvExportService;
    private final AuditService auditService;

    // ── Inventory ───────────────────────────────────────────────────────────

    @GetMapping("/inventory")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Inventory report",
            description = "Totals, per-category breakdown and low/out-of-stock/expiry counts.")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> inventory(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.inventoryReport(categoryId)));
    }

    @GetMapping("/inventory/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Inventory report as CSV")
    public ResponseEntity<byte[]> exportInventory(@RequestParam(required = false) Long categoryId) {
        byte[] csv = csvExportService.inventoryReport(reportService.inventoryReport(categoryId));
        return csvResponse(csv, "inventory-report");
    }

    // ── Donations ───────────────────────────────────────────────────────────

    @GetMapping("/donations")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Donation report",
            description = "Donation and quantity totals, per-donor contributions and a monthly trend.")
    public ResponseEntity<ApiResponse<DonationReportResponse>> donations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long donorId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.donationReport(startDate, endDate, donorId)));
    }

    @GetMapping("/donations/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Donation report as CSV")
    public ResponseEntity<byte[]> exportDonations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long donorId) {
        byte[] csv = csvExportService.donationReport(
                reportService.donationReport(startDate, endDate, donorId));
        return csvResponse(csv, "donation-report");
    }

    // ── Distributions ───────────────────────────────────────────────────────

    @GetMapping("/distributions")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Distribution report",
            description = "Completed totals plus status, priority, beneficiary and item breakdowns.")
    public ResponseEntity<ApiResponse<DistributionReportSummaryResponse>> distributions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long beneficiaryId,
            @RequestParam(required = false) Long itemId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.distributionReport(startDate, endDate, beneficiaryId, itemId)));
    }

    @GetMapping("/distributions/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Distribution report as CSV")
    public ResponseEntity<byte[]> exportDistributions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long beneficiaryId,
            @RequestParam(required = false) Long itemId) {
        byte[] csv = csvExportService.distributionReport(
                reportService.distributionReport(startDate, endDate, beneficiaryId, itemId));
        return csvResponse(csv, "distribution-report");
    }

    // ── Beneficiaries ───────────────────────────────────────────────────────

    @GetMapping("/beneficiaries")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Beneficiary report",
            description = "Counts by status and priority, with aid received per beneficiary.")
    public ResponseEntity<ApiResponse<BeneficiaryReportResponse>> beneficiaries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long beneficiaryId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.beneficiaryReport(startDate, endDate, beneficiaryId)));
    }

    @GetMapping("/beneficiaries/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Beneficiary report as CSV")
    public ResponseEntity<byte[]> exportBeneficiaries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long beneficiaryId) {
        byte[] csv = csvExportService.beneficiaryReport(
                reportService.beneficiaryReport(startDate, endDate, beneficiaryId));
        return csvResponse(csv, "beneficiary-report");
    }

    // ── Volunteers ──────────────────────────────────────────────────────────

    @GetMapping("/volunteers")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Volunteer report", description = "Volunteer counts and task statistics.")
    public ResponseEntity<ApiResponse<VolunteerReportResponse>> volunteers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long volunteerId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.volunteerReport(startDate, endDate, volunteerId)));
    }

    @GetMapping("/volunteers/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Volunteer report as CSV")
    public ResponseEntity<byte[]> exportVolunteers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long volunteerId) {
        byte[] csv = csvExportService.volunteerReport(
                reportService.volunteerReport(startDate, endDate, volunteerId));
        return csvResponse(csv, "volunteer-report");
    }

    // ── Expiry ──────────────────────────────────────────────────────────────

    @GetMapping("/expiry")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Expiry report",
            description = "Expired stock plus items expiring within 7 and 30 days. Expired records are "
                    + "reported, never deleted.")
    public ResponseEntity<ApiResponse<ExpiryReportResponse>> expiry() {
        return ResponseEntity.ok(ApiResponse.success(reportService.expiryReport()));
    }

    @GetMapping("/expiry/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Expiry report as CSV",
            description = "Expired items followed by everything expiring within 30 days.")
    public ResponseEntity<byte[]> exportExpiry() {
        ExpiryReportResponse report = reportService.expiryReport();
        List<InventoryResponse> rows = new ArrayList<>(report.getExpired());
        rows.addAll(report.getExpiringWithin30Days());
        return csvResponse(csvExportService.expiryReport(rows), "expiry-report");
    }

    // ── Stock movements ─────────────────────────────────────────────────────

    @GetMapping("/stock-movements")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Stock movement report",
            description = "The inventory ledger with per-type totals; filterable by item, type, user and dates.")
    public ResponseEntity<ApiResponse<StockMovementReportResponse>> stockMovements(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) Long performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reportService.stockMovementReport(
                itemId, transactionType, referenceType, performedBy, startDate, endDate, pageable)));
    }

    @GetMapping("/stock-movements/export")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Stock movement report as CSV",
            description = "Exports up to 100 movements per request; page through with ?page=.")
    public ResponseEntity<byte[]> exportStockMovements(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) Long performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 100) Pageable pageable) {
        byte[] csv = csvExportService.stockMovementReport(reportService.stockMovementReport(
                itemId, transactionType, referenceType, performedBy, startDate, endDate, pageable));
        return csvResponse(csv, "stock-movement-report");
    }

    private ResponseEntity<byte[]> csvResponse(byte[] csv, String filename) {
        String file = filename + "-" + LocalDate.now() + ".csv";
        auditService.record(AuditAction.EXPORT_REPORT, "Report", filename,
                "Exported " + filename + " as CSV");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(csv);
    }
}
