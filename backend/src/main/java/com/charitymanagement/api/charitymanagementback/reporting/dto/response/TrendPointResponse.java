package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One point of a monthly trend line. Months with no activity are returned as zeroes. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrendPointResponse {

    /** ISO year-month, e.g. {@code 2026-08}. */
    private String period;
    private int year;
    private int month;
    private long count;
    private long quantity;
}
