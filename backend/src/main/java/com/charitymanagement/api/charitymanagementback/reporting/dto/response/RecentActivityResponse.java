package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A recent state-changing action, read from the audit trail. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentActivityResponse {

    private String action;
    private String description;
    private String entityType;
    private String entityId;
    private String performedBy;
    private LocalDateTime occurredAt;
}
