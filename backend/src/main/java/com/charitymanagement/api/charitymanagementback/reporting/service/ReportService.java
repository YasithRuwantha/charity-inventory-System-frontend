package com.charitymanagement.api.charitymanagementback.reporting.service;

import com.charitymanagement.api.charitymanagementback.beneficiary.repository.BeneficiaryRepository;
import com.charitymanagement.api.charitymanagementback.common.config.CharityProperties;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionItemRepository;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionRequestRepository;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonationRepository;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryTransactionResponse;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.inventory.mapper.InventoryMapper;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryTransactionRepository;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryTransactionQueryService;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.BeneficiaryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DistributionReportSummaryResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DonationReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.ExpiryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.InventoryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.ReportBreakdownEntry;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.StockMovementReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.TrendPointResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.VolunteerReportResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerRepository;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only aggregation layer for the reporting API.
 *
 * <p>Every figure is produced by a grouped database query rather than by loading entities and
 * counting them in memory, so a report stays cheap as the tables grow.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<String> MOVEMENT_SORTABLE =
            Set.of("id", "createdAt", "quantity", "transactionType");

    private final InventoryItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionQueryService transactionQueryService;
    private final DonationRepository donationRepository;
    private final DistributionRequestRepository distributionRepository;
    private final DistributionItemRepository distributionItemRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final VolunteerRepository volunteerRepository;
    private final VolunteerTaskRepository taskRepository;
    private final InventoryMapper inventoryMapper;
    private final CharityProperties properties;

    // ── Inventory ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InventoryReportResponse inventoryReport(Long categoryId) {
        LocalDate today = LocalDate.now();
        int horizon = properties.getInventory().getExpiringSoonDays();

        List<ReportBreakdownEntry> categories = itemRepository.aggregateByCategory().stream()
                .filter(row -> categoryId == null || categoryId.equals(toLong(row[0])))
                .map(row -> ReportBreakdownEntry.builder()
                        .id(toLong(row[0]))
                        .label(asString(row[1]))
                        .count(toLong(row[2]))
                        .quantity(toLong(row[3]))
                        .build())
                .toList();

        List<ReportBreakdownEntry> statuses = Arrays.stream(InventoryStatus.values())
                .map(status -> ReportBreakdownEntry.of(status.name(),
                        itemRepository.countByStatusAndCategory(status, categoryId)))
                .toList();

        // Every figure honours ?categoryId=, so a filtered report never reports warehouse-wide
        // totals under a single category's heading.
        return InventoryReportResponse.builder()
                .totalItems(itemRepository.countByCategory(categoryId))
                .totalQuantity(itemRepository.sumQuantityByCategory(categoryId))
                .lowStockItems(itemRepository.countLowStockByCategory(categoryId))
                .outOfStockItems(itemRepository.countOutOfStockByCategory(categoryId))
                .expiredItems(itemRepository.countExpiredByCategory(today, categoryId))
                .expiringSoonItems(itemRepository.countExpiringBetweenByCategory(
                        today, today.plusDays(horizon), categoryId))
                .expiringSoonHorizonDays(horizon)
                .categoryBreakdown(categories)
                .statusBreakdown(statuses)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public ExpiryReportResponse expiryReport() {
        LocalDate today = LocalDate.now();
        List<InventoryResponse> expired = map(itemRepository.findExpired(today));
        List<InventoryResponse> within7 = map(itemRepository.findExpiringBetween(today, today.plusDays(7)));
        List<InventoryResponse> within30 = map(itemRepository.findExpiringBetween(today, today.plusDays(30)));

        return ExpiryReportResponse.builder()
                .expiredCount(expired.size())
                .expiringWithin7DaysCount(within7.size())
                .expiringWithin30DaysCount(within30.size())
                .expired(expired)
                .expiringWithin7Days(within7)
                .expiringWithin30Days(within30)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Donations ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DonationReportResponse donationReport(LocalDate startDate, LocalDate endDate, Long donorId) {
        Object[] totals = firstRow(donationRepository.summarise(DonationStatus.RECEIVED, startDate, endDate));

        List<ReportBreakdownEntry> donors = donationRepository
                .aggregateByDonor(DonationStatus.RECEIVED, startDate, endDate, donorId).stream()
                .map(row -> ReportBreakdownEntry.builder()
                        .id(toLong(row[0]))
                        .code(asString(row[1]))
                        .label(asString(row[2]))
                        .count(toLong(row[3]))
                        .quantity(toLong(row[4]))
                        .build())
                .toList();

        LocalDate trendFrom = startDate != null ? startDate : LocalDate.now().minusMonths(11).withDayOfMonth(1);
        List<TrendPointResponse> trend = monthlyTrend(
                donationRepository.aggregateMonthly(trendFrom, DonationStatus.RECEIVED),
                trendFrom, endDate != null ? endDate : LocalDate.now());

        return DonationReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDonations(toLong(totals[0]))
                .totalQuantityDonated(toLong(totals[1]))
                .totalItemLines(toLong(totals[2]))
                .donorContributions(donors)
                .monthlyTrend(trend)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Distributions ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DistributionReportSummaryResponse distributionReport(LocalDate startDate, LocalDate endDate,
                                                                Long beneficiaryId, Long itemId) {
        Object[] totals = firstRow(distributionRepository.summariseCompleted(startDate, endDate));

        List<ReportBreakdownEntry> statuses = distributionRepository.aggregateByStatus(startDate, endDate)
                .stream()
                .map(row -> ReportBreakdownEntry.of(asString(row[0]), toLong(row[1])))
                .toList();
        List<ReportBreakdownEntry> priorities = distributionRepository.aggregateByPriority(startDate, endDate)
                .stream()
                .map(row -> ReportBreakdownEntry.of(asString(row[0]), toLong(row[1])))
                .toList();
        List<ReportBreakdownEntry> beneficiaries = distributionRepository
                .aggregateByBeneficiary(startDate, endDate, beneficiaryId).stream()
                .map(row -> ReportBreakdownEntry.builder()
                        .id(toLong(row[0]))
                        .code(asString(row[1]))
                        .label(asString(row[2]))
                        .count(toLong(row[3]))
                        .quantity(toLong(row[4]))
                        .build())
                .toList();
        List<ReportBreakdownEntry> items = distributionItemRepository
                .aggregateByItem(startDate, endDate, itemId).stream()
                .map(row -> ReportBreakdownEntry.builder()
                        .id(toLong(row[0]))
                        .code(asString(row[1]))
                        .label(asString(row[2]))
                        .quantity(toLong(row[3]))
                        .build())
                .toList();

        LocalDate trendFrom = startDate != null ? startDate : LocalDate.now().minusMonths(11).withDayOfMonth(1);
        List<TrendPointResponse> trend = monthlyTrend(
                distributionRepository.aggregateMonthly(trendFrom, DistributionStatus.COMPLETED),
                trendFrom, endDate != null ? endDate : LocalDate.now());

        return DistributionReportSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .completedDistributions(toLong(totals[0]))
                .totalQuantityDistributed(toLong(totals[1]))
                .totalItemLines(toLong(totals[2]))
                .statusBreakdown(statuses)
                .priorityBreakdown(priorities)
                .beneficiaryBreakdown(beneficiaries)
                .itemBreakdown(items)
                .monthlyTrend(trend)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Beneficiaries ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BeneficiaryReportResponse beneficiaryReport(LocalDate startDate, LocalDate endDate,
                                                       Long beneficiaryId) {
        List<ReportBreakdownEntry> statuses = beneficiaryRepository.aggregateByStatus().stream()
                .map(row -> ReportBreakdownEntry.of(asString(row[0]), toLong(row[1])))
                .toList();
        List<ReportBreakdownEntry> priorities = beneficiaryRepository.aggregateByPriority().stream()
                .map(row -> ReportBreakdownEntry.of(asString(row[0]), toLong(row[1])))
                .toList();
        List<ReportBreakdownEntry> distributions = distributionRepository
                .aggregateByBeneficiary(startDate, endDate, beneficiaryId).stream()
                .map(row -> ReportBreakdownEntry.builder()
                        .id(toLong(row[0]))
                        .code(asString(row[1]))
                        .label(asString(row[2]))
                        .count(toLong(row[3]))
                        .quantity(toLong(row[4]))
                        .build())
                .toList();

        return BeneficiaryReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalBeneficiaries(beneficiaryRepository.count())
                .activeBeneficiaries(beneficiaryRepository.countByStatus(RecordStatus.ACTIVE))
                .inactiveBeneficiaries(beneficiaryRepository.countByStatus(RecordStatus.INACTIVE))
                .statusBreakdown(statuses)
                .priorityBreakdown(priorities)
                .distributionBreakdown(distributions)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Volunteers ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VolunteerReportResponse volunteerReport(LocalDate startDate, LocalDate endDate, Long volunteerId) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        taskRepository.aggregateByStatus(volunteerId, startDate, endDate)
                .forEach(row -> byStatus.put(asString(row[0]), toLong(row[1])));

        List<ReportBreakdownEntry> statusBreakdown = byStatus.entrySet().stream()
                .map(entry -> ReportBreakdownEntry.of(entry.getKey(), entry.getValue()))
                .toList();

        // One row per volunteer/status pair — folded into a single row per volunteer here.
        Map<Long, ReportBreakdownEntry> perVolunteer = new LinkedHashMap<>();
        for (Object[] row : taskRepository.aggregateByVolunteerAndStatus(volunteerId, startDate, endDate)) {
            Long id = toLong(row[0]);
            long count = toLong(row[4]);
            ReportBreakdownEntry entry = perVolunteer.computeIfAbsent(id, key ->
                    ReportBreakdownEntry.builder()
                            .id(key)
                            .code(asString(row[1]))
                            .label(asString(row[2]))
                            .build());
            entry.setCount(entry.getCount() + count);
            if (TaskStatus.COMPLETED.name().equals(asString(row[3]))) {
                entry.setQuantity(entry.getQuantity() + count);
            }
        }

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return VolunteerReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalVolunteers(volunteerRepository.count())
                .activeVolunteers(volunteerRepository.countByStatus(RecordStatus.ACTIVE))
                .inactiveVolunteers(volunteerRepository.countByStatus(RecordStatus.INACTIVE))
                .totalTasks(total)
                .pendingTasks(byStatus.getOrDefault(TaskStatus.PENDING.name(), 0L))
                .inProgressTasks(byStatus.getOrDefault(TaskStatus.IN_PROGRESS.name(), 0L))
                .completedTasks(byStatus.getOrDefault(TaskStatus.COMPLETED.name(), 0L))
                .cancelledTasks(byStatus.getOrDefault(TaskStatus.CANCELLED.name(), 0L))
                .overdueTasks(taskRepository.countByStatusAndDueDateBefore(TaskStatus.PENDING, LocalDate.now())
                        + taskRepository.countByStatusAndDueDateBefore(TaskStatus.IN_PROGRESS, LocalDate.now()))
                .taskStatusBreakdown(statusBreakdown)
                .volunteerBreakdown(List.copyOf(perVolunteer.values()))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Stock movements ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StockMovementReportResponse stockMovementReport(Long itemId,
                                                           TransactionType transactionType,
                                                           ReferenceType referenceType,
                                                           Long performedBy,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           Pageable pageable) {
        LocalDateTime from = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime to = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        List<ReportBreakdownEntry> byType = new ArrayList<>();
        long totalMovements = 0;
        long quantityIn = 0;
        long quantityOut = 0;
        for (Object[] row : transactionRepository.aggregateByType(from, to)) {
            String type = asString(row[0]);
            long count = toLong(row[1]);
            long quantity = toLong(row[2]);
            byType.add(ReportBreakdownEntry.of(type, count, quantity));
            totalMovements += count;
            if (TransactionType.DONATION_IN.name().equals(type)) {
                quantityIn += quantity;
            } else if (TransactionType.DISTRIBUTION_OUT.name().equals(type)) {
                quantityOut += quantity;
            }
        }

        Pageable safe = PageableUtils.sanitize(pageable, MOVEMENT_SORTABLE, "createdAt");
        Page<InventoryTransactionResponse> movements = transactionQueryService.search(
                itemId, transactionType, referenceType, performedBy, startDate, endDate, safe);

        return StockMovementReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalMovements(totalMovements)
                .totalQuantityIn(quantityIn)
                .totalQuantityOut(quantityOut)
                .typeBreakdown(byType)
                .movements(movements.getContent())
                .pagination(PaginationMeta.of(movements))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Shared helpers ──────────────────────────────────────────────────────

    /**
     * Expands sparse {@code [year, month, count, quantity]} rows into a continuous series so a
     * chart does not silently skip months with no activity.
     */
    List<TrendPointResponse> monthlyTrend(List<Object[]> rows, LocalDate from, LocalDate to) {
        Map<YearMonth, Object[]> byMonth = new LinkedHashMap<>();
        rows.forEach(row -> byMonth.put(YearMonth.of((int) toLong(row[0]), (int) toLong(row[1])), row));

        List<TrendPointResponse> points = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);
        while (!cursor.isAfter(last)) {
            Object[] row = byMonth.get(cursor);
            points.add(TrendPointResponse.builder()
                    .period(cursor.toString())
                    .year(cursor.getYear())
                    .month(cursor.getMonthValue())
                    .count(row == null ? 0 : toLong(row[2]))
                    .quantity(row == null ? 0 : toLong(row[3]))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return points;
    }

    private List<InventoryResponse> map(List<InventoryItem> items) {
        return items.stream().map(inventoryMapper::toResponse).toList();
    }

    private static Object[] firstRow(List<Object[]> rows) {
        return rows.isEmpty() ? new Object[]{0L, 0L, 0L} : rows.get(0);
    }

    static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
