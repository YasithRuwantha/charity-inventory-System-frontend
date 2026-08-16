package com.charitymanagement.api.charitymanagementback.volunteer.mapper;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerTaskResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.Volunteer;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.VolunteerTask;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class VolunteerMapper {

    public VolunteerResponse toResponse(Volunteer volunteer) {
        return VolunteerResponse.builder()
                .id(volunteer.getId())
                .volunteerCode(volunteer.getVolunteerCode())
                .volunteerName(volunteer.getVolunteerName())
                .phone(volunteer.getPhone())
                .email(volunteer.getEmail())
                .address(volunteer.getAddress())
                .status(volunteer.getStatus())
                .joinedDate(volunteer.getJoinedDate())
                .notes(volunteer.getNotes())
                .createdAt(volunteer.getCreatedAt())
                .updatedAt(volunteer.getUpdatedAt())
                .build();
    }

    public VolunteerTaskResponse toResponse(VolunteerTask task) {
        Volunteer volunteer = task.getVolunteer();
        return VolunteerTaskResponse.builder()
                .id(task.getId())
                .volunteerId(volunteer.getId())
                .volunteerCode(volunteer.getVolunteerCode())
                .volunteerName(volunteer.getVolunteerName())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskDate(task.getTaskDate())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
                .assignedBy(UserSummary.from(task.getAssignedBy()))
                .relatedDistributionId(task.getRelatedDistributionId())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .cancelledAt(task.getCancelledAt())
                .cancellationReason(task.getCancellationReason())
                .overdue(isOverdue(task))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private static boolean isOverdue(VolunteerTask task) {
        return task.getDueDate() != null
                && !task.getStatus().isTerminal()
                && task.getDueDate().isBefore(LocalDate.now())
                && task.getStatus() != TaskStatus.CANCELLED;
    }
}
