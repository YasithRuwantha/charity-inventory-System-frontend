package com.charitymanagement.api.charitymanagementback.volunteer.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.CreateVolunteerTaskRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.TaskStatusUpdateRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.UpdateVolunteerTaskRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerTaskResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.service.VolunteerTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/volunteer-tasks")
@RequiredArgsConstructor
@Tag(name = "Volunteer tasks", description = "Task assignment, editing and status transitions")
public class VolunteerTaskController {

    private final VolunteerTaskService taskService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Assign a task to a volunteer")
    public ResponseEntity<ApiResponse<VolunteerTaskResponse>> create(
            @Valid @RequestBody CreateVolunteerTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(taskService.create(request), "Task assigned successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Search volunteer tasks",
            description = "By title, description or volunteer; filterable by volunteer, status and date range.")
    public ResponseEntity<ApiResponse<List<VolunteerTaskResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long volunteerId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<VolunteerTaskResponse> page = taskService.search(search, volunteerId, status, startDate, endDate,
                pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Get one task")
    public ResponseEntity<ApiResponse<VolunteerTaskResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Edit or reassign a task",
            description = "Completed and cancelled tasks are closed and cannot be edited.")
    public ResponseEntity<ApiResponse<VolunteerTaskResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateVolunteerTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.update(id, request),
                "Task updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Move a task through its lifecycle",
            description = "PENDING to IN_PROGRESS to COMPLETED, or CANCELLED with a reason. A VOLUNTEER-role "
                    + "user may only update tasks assigned to their own volunteer record.")
    public ResponseEntity<ApiResponse<VolunteerTaskResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.changeStatus(id, request),
                "Task status updated successfully"));
    }
}
