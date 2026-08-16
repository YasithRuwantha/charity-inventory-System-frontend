package com.charitymanagement.api.charitymanagementback.audit.controller;

import com.charitymanagement.api.charitymanagementback.audit.dto.AuditLogResponse;
import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditLogQueryService;
import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Admin-only view of the audit trail. The log is append-only: there is deliberately no endpoint to
 * edit or delete entries.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit logs", description = "Append-only history of every state-changing action (ADMIN only)")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Search the audit trail",
            description = "Filter by user, action, entity and date range; newest first by default. Passwords, "
                    + "password hashes and tokens are never recorded, so they can never appear here.")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AuditLogResponse> page = auditLogQueryService.search(
                userId, action, entityType, entityId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/actions")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "List the auditable action names",
            description = "The accepted values for the ?action= filter.")
    public ResponseEntity<ApiResponse<List<String>>> actions() {
        return ResponseEntity.ok(ApiResponse.success(
                Arrays.stream(AuditAction.values()).map(Enum::name).toList()));
    }
}
