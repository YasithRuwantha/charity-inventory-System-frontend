package com.charitymanagement.api.charitymanagementback.reporting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private long totalVolunteers;
    private long activeVolunteers;
    private long inactiveVolunteers;

    private long totalTasks;
    private long pendingTasks;
    private long inProgressTasks;
    private long completedTasks;
    private long cancelledTasks;
    private long overdueTasks;

    private List<ReportBreakdownEntry> taskStatusBreakdown;
    /** Task counts per volunteer; {@code count} is total tasks, {@code quantity} completed ones. */
    private List<ReportBreakdownEntry> volunteerBreakdown;

    private LocalDateTime generatedAt;
}
