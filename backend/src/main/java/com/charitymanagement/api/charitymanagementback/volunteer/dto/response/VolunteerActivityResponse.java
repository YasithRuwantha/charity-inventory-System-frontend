package com.charitymanagement.api.charitymanagementback.volunteer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One entry in a volunteer's chronological activity feed, reconstructed from the lifecycle
 * timestamps of their tasks.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerActivityResponse {

    /** TASK_ASSIGNED, TASK_STARTED, TASK_COMPLETED or TASK_CANCELLED. */
    private String activityType;
    private String description;
    private Long taskId;
    private String taskTitle;
    private Long relatedDistributionId;
    private LocalDateTime occurredAt;
}
