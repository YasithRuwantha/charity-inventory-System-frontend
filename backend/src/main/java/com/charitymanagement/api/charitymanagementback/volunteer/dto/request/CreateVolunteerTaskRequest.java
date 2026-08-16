package com.charitymanagement.api.charitymanagementback.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CreateVolunteerTaskRequest {

    @NotNull(message = "Volunteer id is required")
    private Long volunteerId;

    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Task date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate taskDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    /** Optional link to the aid distribution this task supports. */
    private Long relatedDistributionId;
}
