package com.charitymanagement.api.charitymanagementback.volunteer.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.dto.request.StatusUpdateRequest;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.CreateVolunteerRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.UpdateVolunteerRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerActivityResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerTaskResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.service.VolunteerService;
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
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
@Tag(name = "Volunteers", description = "Volunteer registry, assigned tasks and activity history")
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final VolunteerTaskService taskService;

    @PostMapping
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Register a volunteer",
            description = "Volunteer codes (VOL-000001) are generated.")
    public ResponseEntity<ApiResponse<VolunteerResponse>> create(
            @Valid @RequestBody CreateVolunteerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(volunteerService.create(request),
                        "Volunteer registered successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Search volunteers",
            description = "By code, name, email or phone; filterable by status and paged.")
    public ResponseEntity<ApiResponse<List<VolunteerResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<VolunteerResponse> page = volunteerService.search(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Get one volunteer")
    public ResponseEntity<ApiResponse<VolunteerResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(volunteerService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Update a volunteer")
    public ResponseEntity<ApiResponse<VolunteerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateVolunteerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(volunteerService.update(id, request),
                "Volunteer updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Activate or deactivate a volunteer",
            description = "Inactive volunteers keep their history but cannot receive new tasks.")
    public ResponseEntity<ApiResponse<VolunteerResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(volunteerService.changeStatus(id, request.getStatus()),
                "Volunteer status updated successfully"));
    }

    @GetMapping("/{id}/tasks")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Tasks assigned to a volunteer",
            description = "A VOLUNTEER-role user may only read the record matching their own account email.")
    public ResponseEntity<ApiResponse<List<VolunteerTaskResponse>>> tasks(
            @PathVariable Long id,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        volunteerService.assertCanAccess(volunteerService.requireVolunteer(id));
        Page<VolunteerTaskResponse> page = taskService.search(null, id, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "Chronological activity for a volunteer",
            description = "Task assignments, starts, completions and cancellations, newest first.")
    public ResponseEntity<ApiResponse<List<VolunteerActivityResponse>>> activity(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getActivity(id)));
    }
}
