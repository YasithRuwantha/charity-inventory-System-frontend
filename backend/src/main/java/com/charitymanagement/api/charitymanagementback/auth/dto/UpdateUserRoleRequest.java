package com.charitymanagement.api.charitymanagementback.auth.dto;

import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}
