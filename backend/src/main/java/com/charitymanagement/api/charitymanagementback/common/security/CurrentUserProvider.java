package com.charitymanagement.api.charitymanagementback.common.security;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.repository.UserRepository;
import com.charitymanagement.api.charitymanagementback.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Single source of truth for "who is calling". Every write path takes the acting user from here —
 * a user id or role sent in a request body is never trusted.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    /** The authenticated principal, detached from the persistence context. */
    public User requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("No authenticated user is bound to this request");
        }
        return user;
    }

    public Optional<User> findPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /**
     * The acting user as a managed entity, safe to assign to a {@code @ManyToOne} association.
     * Must be called inside an active transaction.
     */
    public User requireManagedUser() {
        Long id = requirePrincipal().getId();
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
    }

    public boolean isAdmin() {
        return findPrincipal().map(u -> u.getRole() == UserRole.ADMIN).orElse(false);
    }
}
