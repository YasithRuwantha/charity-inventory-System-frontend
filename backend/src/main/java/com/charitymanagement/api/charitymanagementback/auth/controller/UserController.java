package com.charitymanagement.api.charitymanagementback.auth.controller;

import com.charitymanagement.api.charitymanagementback.auth.dto.UpdateUserRoleRequest;
import com.charitymanagement.api.charitymanagementback.auth.dto.UpdateUserStatusRequest;
import com.charitymanagement.api.charitymanagementback.auth.dto.UserResponse;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import com.charitymanagement.api.charitymanagementback.auth.service.UserManagementService;
import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administration of existing accounts. Registration and login stay on {@code /api/v1/auth} — this
 * controller never creates accounts, hashes passwords or issues tokens.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Account administration: listing, role changes and activation")
public class UserController {

    private final UserManagementService userManagementService;

    @GetMapping("/me")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    @Operation(summary = "The authenticated user",
            description = "Resolved from the JWT in the SecurityContext, never from a request parameter.")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.currentUser()));
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "List users",
            description = "Search by name or email, filter by role and status, paged. Password hashes are "
                    + "never returned.")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> page = userManagementService.search(search, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Get one user")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.get(id)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Change a user's role",
            description = "Demoting the last active administrator is refused with 409 LAST_ADMIN_PROTECTED.")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userManagementService.changeRole(id, request.getRole()),
                "User role updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Activate or deactivate a user",
            description = "A deactivated account keeps all of its history but can no longer log in. Callers "
                    + "cannot deactivate themselves, and the last active administrator is protected.")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userManagementService.changeStatus(id, request.getStatus()),
                "User status updated successfully"));
    }
}
