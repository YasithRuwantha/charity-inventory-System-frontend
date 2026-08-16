package com.charitymanagement.api.charitymanagementback.auth.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.auth.dto.UserResponse;
import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import com.charitymanagement.api.charitymanagementback.auth.repository.UserRepository;
import com.charitymanagement.api.charitymanagementback.common.exception.ConflictException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Administration of existing accounts. Account creation stays with {@code AuthService} so there is
 * exactly one place that hashes passwords and issues tokens — this service never touches either.
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);
    private static final Set<String> SORTABLE = Set.of("id", "name", "email", "role", "status");

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String search, UserRole role, UserStatus status, Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "id");
        String term = search == null || search.isBlank() ? null : search.trim();
        return userRepository.search(term, role, status, safe).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return UserResponse.from(currentUserProvider.requirePrincipal());
    }

    /**
     * Changes a user's role. Demoting the last remaining active administrator is refused, because
     * it would leave nobody able to administer the system.
     */
    @Transactional
    public UserResponse changeRole(Long id, UserRole newRole) {
        User user = require(id);
        UserRole previous = user.getRole();
        if (previous == newRole) {
            return UserResponse.from(user);
        }
        if (previous == UserRole.ADMIN && isLastActiveAdmin(user)) {
            throw new ConflictException("The last active administrator cannot be demoted",
                    "LAST_ADMIN_PROTECTED");
        }
        user.setRole(newRole);
        userRepository.save(user);

        auditService.record(AuditAction.CHANGE_USER_ROLE, "User", id,
                "Changed role of " + user.getEmail() + " from " + previous + " to " + newRole,
                Map.of("role", String.valueOf(previous)), Map.of("role", newRole.name()));
        log.info("Role of user {} changed from {} to {}", id, previous, newRole);
        return UserResponse.from(user);
    }

    /**
     * Activates or deactivates an account. Deactivating flips {@code isEnabled()} on the existing
     * {@code User}, so the next authentication attempt is rejected by the unchanged login flow.
     */
    @Transactional
    public UserResponse changeStatus(Long id, UserStatus newStatus) {
        User user = require(id);
        UserStatus previous = user.getStatus();
        if (previous == newStatus) {
            return UserResponse.from(user);
        }
        if (newStatus == UserStatus.INACTIVE) {
            if (currentUserProvider.requirePrincipal().getId().equals(id)) {
                throw new ConflictException("You cannot deactivate your own account",
                        "SELF_DEACTIVATION_BLOCKED");
            }
            if (user.getRole() == UserRole.ADMIN && isLastActiveAdmin(user)) {
                throw new ConflictException("The last active administrator cannot be deactivated",
                        "LAST_ADMIN_PROTECTED");
            }
        }
        user.setStatus(newStatus);
        userRepository.save(user);

        auditService.record(AuditAction.USER_STATUS_CHANGE, "User", id,
                "Changed status of " + user.getEmail() + " from " + previous + " to " + newStatus,
                Map.of("status", previous.name()), Map.of("status", newStatus.name()));
        log.info("Status of user {} changed from {} to {}", id, previous, newStatus);
        return UserResponse.from(user);
    }

    /** True when this user is currently the only enabled ADMIN account. */
    private boolean isLastActiveAdmin(User user) {
        return user.getStatus() == UserStatus.ACTIVE && userRepository.countActiveAdmins() <= 1;
    }

    private User require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
