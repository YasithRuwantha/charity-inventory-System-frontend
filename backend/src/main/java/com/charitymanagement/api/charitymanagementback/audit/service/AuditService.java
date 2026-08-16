package com.charitymanagement.api.charitymanagementback.audit.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.entity.AuditLog;
import com.charitymanagement.api.charitymanagementback.audit.repository.AuditLogRepository;
import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Writes audit entries in the caller's transaction, so an action that rolls back leaves no
 * "it happened" record behind.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    /** Any key containing one of these fragments is redacted before it reaches the database. */
    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of(
            "password", "passwd", "secret", "token", "jwt", "credential", "authorization", "hash");

    private static final int MAX_JSON_LENGTH = 8000;

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public void record(AuditAction action, String entityType, Object entityId, String description) {
        record(action, entityType, entityId, description, null, null);
    }

    public void record(AuditAction action, String entityType, Object entityId, String description,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            User actor = currentUserProvider.findPrincipal().orElse(null);
            AuditLog entry = AuditLog.builder()
                    .userId(actor != null ? actor.getId() : null)
                    .userEmail(actor != null ? actor.getEmail() : null)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId != null ? String.valueOf(entityId) : null)
                    .description(truncate(description, 1000))
                    .oldValues(toJson(oldValues))
                    .newValues(toJson(newValues))
                    .ipAddress(resolveClientIp())
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (RuntimeException ex) {
            // Auditing must never be the reason a valid business operation fails.
            log.error("Failed to write audit entry for action {} on {}#{}", action, entityType, entityId, ex);
        }
    }

    private String toJson(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        values.forEach((key, value) -> sanitized.put(key, isSensitive(key) ? "[REDACTED]" : value));
        try {
            return truncate(objectMapper.writeValueAsString(sanitized), MAX_JSON_LENGTH);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialise audit payload: {}", ex.getMessage());
            return null;
        }
    }

    private static boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(lower::contains);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    private static String resolveClientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
