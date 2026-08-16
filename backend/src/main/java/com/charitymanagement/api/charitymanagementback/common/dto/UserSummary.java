package com.charitymanagement.api.charitymanagementback.common.dto;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Safe projection of a user for embedding in responses — never carries the password hash. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSummary {

    private Long id;
    private String name;
    private String email;
    private String role;

    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
