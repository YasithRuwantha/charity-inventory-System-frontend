package com.charitymanagement.api.charitymanagementback.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unauthenticated liveness probe. It reports only whether the app is up and whether charity_db
 * answers — never the JDBC URL, credentials or driver details.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Health", description = "Public liveness and database connectivity probe")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "Application health",
            description = "Returns 200 with status UP when the database responds, 503 with status DOWN "
                    + "otherwise. No connection details are disclosed either way.")
    public ResponseEntity<Map<String, Object>> health() {
        boolean databaseUp = pingDatabase();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", databaseUp ? "UP" : "DOWN");
        body.put("database", databaseUp ? "CONNECTED" : "DISCONNECTED");
        body.put("timestamp", LocalDateTime.now());

        return databaseUp
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean pingDatabase() {
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            return true;
        } catch (RuntimeException ex) {
            // Logged in full for operators; the response stays deliberately vague.
            log.error("Health check could not reach the database", ex);
            return false;
        }
    }
}
