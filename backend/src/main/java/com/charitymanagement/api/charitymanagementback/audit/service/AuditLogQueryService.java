package com.charitymanagement.api.charitymanagementback.audit.service;

import com.charitymanagement.api.charitymanagementback.audit.dto.AuditLogResponse;
import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.repository.AuditLogRepository;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Read side of the audit trail. Kept separate from {@link AuditService} so the write path stays
 * free of query concerns and can never be slowed down by reporting work.
 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "action", "entityType", "userId", "id");

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(Long userId, AuditAction action, String entityType, String entityId,
                                         LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate must not be after endDate");
        }
        // Dates arrive as plain days; widen them to cover the whole calendar day.
        LocalDateTime from = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime to = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "createdAt");
        return auditLogRepository
                .search(userId, action, blankToNull(entityType), blankToNull(entityId), from, to, safe)
                .map(AuditLogResponse::from);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
