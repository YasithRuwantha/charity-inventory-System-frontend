package com.charitymanagement.api.charitymanagementback.auth.dto;

import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    /** Self-service roles only: INVENTORY_STAFF or VOLUNTEER. ADMIN is rejected by AuthService. */
    private UserRole role;
}