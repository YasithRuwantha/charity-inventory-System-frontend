package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardAlertResponse {

    /** CRITICAL, WARNING or INFO. */
    private String severity;
    /** EXPIRED_STOCK, OUT_OF_STOCK, LOW_STOCK, EXPIRING_SOON, PENDING_DISTRIBUTIONS, … */
    private String type;
    private String message;
    private long count;
    private String entityType;
    private Long entityId;
}
