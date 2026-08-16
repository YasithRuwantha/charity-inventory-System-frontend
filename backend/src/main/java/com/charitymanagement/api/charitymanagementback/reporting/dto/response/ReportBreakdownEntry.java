package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of any grouped report figure — a category, a status, a donor, a beneficiary or an item.
 * Kept generic so every breakdown in the reporting API reads the same way to a client.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportBreakdownEntry {

    private Long id;
    private String code;
    private String label;
    private long count;
    private long quantity;

    public static ReportBreakdownEntry of(String label, long count) {
        return ReportBreakdownEntry.builder().label(label).count(count).build();
    }

    public static ReportBreakdownEntry of(String label, long count, long quantity) {
        return ReportBreakdownEntry.builder().label(label).count(count).quantity(quantity).build();
    }
}
