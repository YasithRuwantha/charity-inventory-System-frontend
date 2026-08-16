package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpiryReportResponse {

    private long expiredCount;
    private long expiringWithin7DaysCount;
    private long expiringWithin30DaysCount;

    /** Expired stock is reported, never deleted — the history has to stay auditable. */
    private List<InventoryResponse> expired;
    private List<InventoryResponse> expiringWithin7Days;
    private List<InventoryResponse> expiringWithin30Days;

    private LocalDateTime generatedAt;
}
