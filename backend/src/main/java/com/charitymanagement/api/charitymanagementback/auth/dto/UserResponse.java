package com.charitymanagement.api.charitymanagementback.auth.dto;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Administrative view of an account. The password hash is deliberately absent. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String status;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                // getStatus() normalises a legacy null to ACTIVE.
                .status(user.getStatus().name())
                .build();
    }
}
