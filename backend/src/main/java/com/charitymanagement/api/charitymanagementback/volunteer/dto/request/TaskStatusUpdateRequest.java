package com.charitymanagement.api.charitymanagementback.volunteer.dto.request;

import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;

    /** Required when moving a task to CANCELLED. */
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
