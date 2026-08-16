package com.charitymanagement.api.charitymanagementback.reporting.service;

import com.charitymanagement.api.charitymanagementback.audit.repository.AuditLogRepository;
import com.charitymanagement.api.charitymanagementback.beneficiary.repository.BeneficiaryRepository;
import com.charitymanagement.api.charitymanagementback.common.config.CharityProperties;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionRequestRepository;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonationRepository;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonorRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryCategoryRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemRepository;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DashboardAlertResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DashboardSummaryResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.RecentActivityResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.ReportBreakdownEntry;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.TrendPointResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerRepository;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backing queries for a future dashboard UI. Nothing here is hard-coded: every number is counted
 * from charity_db when the endpoint is called.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InventoryItemRepository itemRepository;
    private final InventoryCategoryRepository categoryRepository;
    private final DonationRepository donationRepository;
    private final DonorRepository donorRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final DistributionRequestRepository distributionRepository;
    private final VolunteerRepository volunteerRepository;
    private final VolunteerTaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReportService reportService;
    private final CharityProperties properties;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        int horizon = properties.getInventory().getExpiringSoonDays();

        return DashboardSummaryResponse.builder()
                .totalInventoryItems(itemRepository.countByArchivedFalse())
                .totalStockUnits(itemRepository.sumTotalQuantity())
                .lowStockItems(itemRepository.countLowStock())
                .outOfStockItems(itemRepository.countOutOfStock())
                .expiredItems(itemRepository.countExpired(today))
                .expiringSoonItems(itemRepository.countExpiringBetween(today, today.plusDays(horizon)))
                .totalCategories(categoryRepository.count())
                .totalDonors(donorRepository.count())
                .totalBeneficiaries(beneficiaryRepository.count())
                .totalVolunteers(volunteerRepository.count())
                .donationsThisMonth(donationRepository.countByDonationDateBetween(monthStart, monthEnd))
                .distributionsThisMonth(distributionRepository.countByStatusAndRequestDateBetween(
                        DistributionStatus.COMPLETED, monthStart, monthEnd))
                .pendingDistributionRequests(distributionRepository.countByStatus(DistributionStatus.PENDING))
                .approvedDistributionRequests(distributionRepository.countByStatus(DistributionStatus.APPROVED))
                .pendingVolunteerTasks(taskRepository.countByStatus(TaskStatus.PENDING))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrendPointResponse> donationTrend(String period) {
        LocalDate from = periodStart(period);
        return reportService.monthlyTrend(
                donationRepository.aggregateMonthly(from, DonationStatus.RECEIVED), from, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<TrendPointResponse> distributionTrend(String period) {
        LocalDate from = periodStart(period);
        return reportService.monthlyTrend(
                distributionRepository.aggregateMonthly(from, DistributionStatus.COMPLETED), from,
                LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ReportBreakdownEntry> inventoryByCategory() {
        return itemRepository.aggregateByCategory().stream()
                .map(row -> ReportBreakdownEntry.builder()
                        .id(ReportService.toLong(row[0]))
                        .label(ReportService.asString(row[1]))
                        .count(ReportService.toLong(row[2]))
                        .quantity(ReportService.toLong(row[3]))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportBreakdownEntry> beneficiaryPriority() {
        return beneficiaryRepository.aggregateByPriority().stream()
                .map(row -> ReportBreakdownEntry.of(ReportService.asString(row[0]),
                        ReportService.toLong(row[1])))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentActivityResponse> recentActivity() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(entry -> RecentActivityResponse.builder()
                        .action(entry.getAction() != null ? entry.getAction().name() : null)
                        .description(entry.getDescription())
                        .entityType(entry.getEntityType())
                        .entityId(entry.getEntityId())
                        .performedBy(entry.getUserEmail())
                        .occurredAt(entry.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Operational warnings, most urgent first. Expired and out-of-stock items are CRITICAL because
     * they block distributions outright; the rest are advisory.
     */
    @Transactional(readOnly = true)
    public List<DashboardAlertResponse> alerts() {
        LocalDate today = LocalDate.now();
        int horizon = properties.getInventory().getExpiringSoonDays();
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        long expired = itemRepository.countExpired(today);
        if (expired > 0) {
            alerts.add(alert("CRITICAL", "EXPIRED_STOCK",
                    expired + " inventory item(s) have expired and cannot be distributed", expired));
        }
        long outOfStock = itemRepository.countOutOfStock();
        if (outOfStock > 0) {
            alerts.add(alert("CRITICAL", "OUT_OF_STOCK", outOfStock + " inventory item(s) are out of stock",
                    outOfStock));
        }
        long lowStock = itemRepository.countLowStock();
        if (lowStock > 0) {
            alerts.add(alert("WARNING", "LOW_STOCK",
                    lowStock + " inventory item(s) are at or below their minimum stock level", lowStock));
        }
        long expiringSoon = itemRepository.countExpiringBetween(today, today.plusDays(horizon));
        if (expiringSoon > 0) {
            alerts.add(alert("WARNING", "EXPIRING_SOON",
                    expiringSoon + " inventory item(s) expire within " + horizon + " days", expiringSoon));
        }
        long pending = distributionRepository.countByStatus(DistributionStatus.PENDING);
        if (pending > 0) {
            alerts.add(alert("INFO", "PENDING_DISTRIBUTIONS",
                    pending + " distribution request(s) are awaiting approval", pending));
        }
        long overdueTasks = taskRepository.countByStatusAndDueDateBefore(TaskStatus.PENDING, today)
                + taskRepository.countByStatusAndDueDateBefore(TaskStatus.IN_PROGRESS, today);
        if (overdueTasks > 0) {
            alerts.add(alert("WARNING", "OVERDUE_TASKS", overdueTasks + " volunteer task(s) are past their due date",
                    overdueTasks));
        }

        // High-priority requests are surfaced individually so they can be actioned directly.
        for (DistributionRequest request : distributionRepository
                .findTop10ByStatusOrderByRequestDateDesc(DistributionStatus.PENDING)) {
            if (request.getPriority() != null && "HIGH".equals(request.getPriority().name())) {
                alerts.add(DashboardAlertResponse.builder()
                        .severity("CRITICAL")
                        .type("HIGH_PRIORITY_REQUEST")
                        .message("High-priority request " + request.getRequestReference() + " for "
                                + request.getBeneficiary().getBeneficiaryName() + " is still pending")
                        .count(1)
                        .entityType("DistributionRequest")
                        .entityId(request.getId())
                        .build());
            }
        }
        return alerts;
    }

    /** Accepts {@code 3months}, {@code 6months} or {@code 12months}; defaults to 6. */
    private static LocalDate periodStart(String period) {
        int months = switch (period == null ? "6months" : period.trim().toLowerCase(Locale.ROOT)) {
            case "3months" -> 3;
            case "6months", "" -> 6;
            case "12months" -> 12;
            default -> throw new BadRequestException(
                    "Unsupported period '" + period + "'. Use 3months, 6months or 12months.");
        };
        return LocalDate.now().minusMonths(months - 1L).withDayOfMonth(1);
    }

    private static DashboardAlertResponse alert(String severity, String type, String message, long count) {
        return DashboardAlertResponse.builder()
                .severity(severity)
                .type(type)
                .message(message)
                .count(count)
                .build();
    }
}
