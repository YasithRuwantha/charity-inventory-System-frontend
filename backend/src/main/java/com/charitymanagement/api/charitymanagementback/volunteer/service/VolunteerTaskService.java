package com.charitymanagement.api.charitymanagementback.volunteer.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.exception.ConflictException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.CreateVolunteerTaskRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.TaskStatusUpdateRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.UpdateVolunteerTaskRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerActivityResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerTaskResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.Volunteer;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.VolunteerTask;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.mapper.VolunteerMapper;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerSpecifications;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VolunteerTaskService {

    private static final Logger log = LoggerFactory.getLogger(VolunteerTaskService.class);

    private static final Set<String> SORTABLE = Set.of(
            "id", "title", "taskDate", "dueDate", "status", "completedAt", "createdAt", "updatedAt");

    private final VolunteerTaskRepository taskRepository;
    private final VolunteerService volunteerService;
    private final VolunteerMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional
    public VolunteerTaskResponse create(CreateVolunteerTaskRequest request) {
        Volunteer volunteer = volunteerService.requireVolunteer(request.getVolunteerId());
        if (volunteer.getStatus() != RecordStatus.ACTIVE) {
            throw new BadRequestException("Volunteer " + volunteer.getVolunteerCode()
                    + " is inactive and cannot be assigned tasks", "VOLUNTEER_INACTIVE");
        }
        assertDueDateNotBeforeTaskDate(request.getTaskDate(), request.getDueDate());

        VolunteerTask task = taskRepository.save(VolunteerTask.builder()
                .volunteer(volunteer)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .taskDate(request.getTaskDate())
                .dueDate(request.getDueDate())
                .status(TaskStatus.PENDING)
                .assignedBy(currentUserProvider.requireManagedUser())
                .relatedDistributionId(request.getRelatedDistributionId())
                .build());

        auditService.record(AuditAction.ASSIGN_VOLUNTEER_TASK, "VolunteerTask", task.getId(),
                "Assigned task '" + task.getTitle() + "' to " + volunteer.getVolunteerName(),
                null, snapshot(task));
        log.info("Assigned task {} to volunteer {}", task.getId(), volunteer.getVolunteerCode());
        return mapper.toResponse(task);
    }

    @Transactional
    public VolunteerTaskResponse update(Long id, UpdateVolunteerTaskRequest request) {
        VolunteerTask task = requireTask(id);
        if (task.getStatus().isTerminal()) {
            throw new ConflictException("Task " + id + " is " + task.getStatus() + " and can no longer be edited",
                    "TASK_ALREADY_CLOSED");
        }
        Map<String, Object> before = snapshot(task);
        assertDueDateNotBeforeTaskDate(request.getTaskDate(), request.getDueDate());

        if (request.getVolunteerId() != null
                && !request.getVolunteerId().equals(task.getVolunteer().getId())) {
            Volunteer reassigned = volunteerService.requireVolunteer(request.getVolunteerId());
            if (reassigned.getStatus() != RecordStatus.ACTIVE) {
                throw new BadRequestException("Volunteer " + reassigned.getVolunteerCode()
                        + " is inactive and cannot be assigned tasks", "VOLUNTEER_INACTIVE");
            }
            task.setVolunteer(reassigned);
        }
        task.setTitle(request.getTitle().trim());
        task.setDescription(trimToNull(request.getDescription()));
        task.setTaskDate(request.getTaskDate());
        task.setDueDate(request.getDueDate());
        task.setRelatedDistributionId(request.getRelatedDistributionId());
        taskRepository.save(task);

        auditService.record(AuditAction.UPDATE_VOLUNTEER_TASK, "VolunteerTask", id,
                "Updated task '" + task.getTitle() + "'", before, snapshot(task));
        return mapper.toResponse(task);
    }

    /**
     * Moves a task along its lifecycle. Transitions are restricted by
     * {@link TaskStatus#allowedTransitions()}, so a finished task can never be reopened and the
     * activity feed stays a truthful history.
     */
    @Transactional
    public VolunteerTaskResponse changeStatus(Long id, TaskStatusUpdateRequest request) {
        VolunteerTask task = requireTask(id);
        volunteerService.assertCanAccess(task.getVolunteer());

        TaskStatus current = task.getStatus();
        TaskStatus target = request.getStatus();
        if (current == target) {
            return mapper.toResponse(task);
        }
        if (!current.allowedTransitions().contains(target)) {
            throw new ConflictException("A " + current + " task cannot move to " + target,
                    "INVALID_TASK_TRANSITION");
        }
        String reason = trimToNull(request.getReason());
        if (target == TaskStatus.CANCELLED && reason == null) {
            throw new BadRequestException("A reason is required to cancel a task", "CANCELLATION_REASON_REQUIRED");
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(target);
        switch (target) {
            case IN_PROGRESS -> task.setStartedAt(now);
            case COMPLETED -> {
                task.setCompletedAt(now);
                // A task completed straight from PENDING still needs a start time for the feed.
                if (task.getStartedAt() == null) {
                    task.setStartedAt(now);
                }
            }
            case CANCELLED -> {
                task.setCancelledAt(now);
                task.setCancellationReason(reason);
            }
            default -> { /* PENDING is never a transition target */ }
        }
        taskRepository.save(task);

        auditService.record(AuditAction.CHANGE_VOLUNTEER_TASK_STATUS, "VolunteerTask", id,
                "Task '" + task.getTitle() + "' moved from " + current + " to " + target,
                Map.of("status", current),
                reason == null ? Map.of("status", target) : Map.of("status", target, "reason", reason));

        log.info("Volunteer task {} moved {} -> {}", id, current, target);
        return mapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public VolunteerTaskResponse get(Long id) {
        return mapper.toResponse(requireTask(id));
    }

    @Transactional(readOnly = true)
    public Page<VolunteerTaskResponse> search(String search,
                                              Long volunteerId,
                                              TaskStatus status,
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "taskDate");
        return taskRepository
                .findAll(VolunteerSpecifications.tasks(search, volunteerId, status, startDate, endDate), safe)
                .map(mapper::toResponse);
    }

    /**
     * Chronological feed for one volunteer, newest first. Entries are derived from each task's
     * lifecycle timestamps rather than a separate log table, so the feed cannot drift from the
     * tasks it describes.
     */
    @Transactional(readOnly = true)
    public List<VolunteerActivityResponse> getActivity(Long volunteerId) {
        Volunteer volunteer = volunteerService.requireVolunteer(volunteerId);
        volunteerService.assertCanAccess(volunteer);

        List<VolunteerActivityResponse> activity = new ArrayList<>();
        for (VolunteerTask task : taskRepository.findByVolunteerIdOrderByTaskDateDesc(volunteerId)) {
            activity.add(entry("TASK_ASSIGNED", "Task '" + task.getTitle() + "' was assigned", task,
                    task.getCreatedAt()));
            if (task.getStartedAt() != null) {
                activity.add(entry("TASK_STARTED", "Work started on '" + task.getTitle() + "'", task,
                        task.getStartedAt()));
            }
            if (task.getCompletedAt() != null) {
                activity.add(entry("TASK_COMPLETED", "Task '" + task.getTitle() + "' was completed", task,
                        task.getCompletedAt()));
            }
            if (task.getCancelledAt() != null) {
                String reason = task.getCancellationReason() == null ? "" : ": " + task.getCancellationReason();
                activity.add(entry("TASK_CANCELLED", "Task '" + task.getTitle() + "' was cancelled" + reason,
                        task, task.getCancelledAt()));
            }
        }
        activity.sort(Comparator.comparing(VolunteerActivityResponse::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return activity;
    }

    @Transactional(readOnly = true)
    public VolunteerTask requireTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer task", id));
    }

    private static VolunteerActivityResponse entry(String type, String description, VolunteerTask task,
                                                   LocalDateTime occurredAt) {
        return VolunteerActivityResponse.builder()
                .activityType(type)
                .description(description)
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .relatedDistributionId(task.getRelatedDistributionId())
                .occurredAt(occurredAt)
                .build();
    }

    private static void assertDueDateNotBeforeTaskDate(LocalDate taskDate, LocalDate dueDate) {
        if (dueDate != null && taskDate != null && dueDate.isBefore(taskDate)) {
            throw new BadRequestException("Due date cannot be before the task date");
        }
    }

    private static Map<String, Object> snapshot(VolunteerTask task) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("volunteerId", task.getVolunteer() != null ? task.getVolunteer().getId() : null);
        values.put("title", task.getTitle());
        values.put("taskDate", task.getTaskDate());
        values.put("dueDate", task.getDueDate());
        values.put("status", task.getStatus());
        values.put("relatedDistributionId", task.getRelatedDistributionId());
        return values;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
