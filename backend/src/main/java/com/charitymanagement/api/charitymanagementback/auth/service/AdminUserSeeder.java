package com.charitymanagement.api.charitymanagementback.auth.service;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import com.charitymanagement.api.charitymanagementback.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the bootstrap administrator on first start when no account with the configured email
 * exists. Existing rows are never modified (password included), so restarts are safe.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "charity.seed.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${charity.seed.admin.email:admin@charity.local}")
    private String email;

    @Value("${charity.seed.admin.password:Admin@123}")
    private String password;

    @Value("${charity.seed.admin.name:System Administrator}")
    private String name;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        userRepository.save(User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        log.info("Seeded bootstrap administrator account ({})", email);
    }
}
