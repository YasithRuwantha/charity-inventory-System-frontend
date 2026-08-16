package com.charitymanagement.api.charitymanagementback.volunteer.dto.response;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerTaskResponse {

    private Long id;
    private Long volunteerId;
    private String volunteerCode;
    private String volunteerName;
    private String title;
    private String description;
    private LocalDate taskDate;
    private LocalDate dueDate;
    private TaskStatus status;
    private UserSummary assignedBy;
    private Long relatedDistributionId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private boolean overdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
