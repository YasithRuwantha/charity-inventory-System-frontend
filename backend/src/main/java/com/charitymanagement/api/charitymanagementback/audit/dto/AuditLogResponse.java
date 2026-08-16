package com.charitymanagement.api.charitymanagementback.audit.dto;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Read model for an audit entry. The stored payloads are already sanitised by
 * {@code AuditService}, so nothing secret can reach this DTO.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog entry) {
        return AuditLogResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .userEmail(entry.getUserEmail())
                .action(entry.getAction() != null ? entry.getAction().name() : null)
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .description(entry.getDescription())
                .oldValues(entry.getOldValues())
                .newValues(entry.getNewValues())
                .ipAddress(entry.getIpAddress())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
