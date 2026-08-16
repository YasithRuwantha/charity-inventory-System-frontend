package com.charitymanagement.api.charitymanagementback.auth.controller;

import com.charitymanagement.api.charitymanagementback.auth.dto.AuthResponse;
import com.charitymanagement.api.charitymanagementback.auth.dto.LoginRequest;
import com.charitymanagement.api.charitymanagementback.auth.dto.RegisterRequest;
import com.charitymanagement.api.charitymanagementback.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Body: { "name": "...", "email": "...", "password": "...", "role": "ADMIN|INVENTORY_STAFF|VOLUNTEER" }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * POST /api/v1/auth/login
     * Body: { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
