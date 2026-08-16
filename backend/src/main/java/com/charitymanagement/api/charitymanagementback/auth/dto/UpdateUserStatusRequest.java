package com.charitymanagement.api.charitymanagementback.auth.dto;

import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;
}
