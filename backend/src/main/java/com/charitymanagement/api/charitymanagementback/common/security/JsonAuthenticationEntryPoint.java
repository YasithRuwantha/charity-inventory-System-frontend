package com.charitymanagement.api.charitymanagementback.common.security;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns the standard JSON error envelope for missing/invalid credentials instead of Spring's
 * default empty 401/403 bodies.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String reason = (String) request.getAttribute(JwtErrorAttributes.JWT_ERROR_ATTRIBUTE);
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                reason != null ? reason : "Authentication is required to access this resource",
                reason != null ? "INVALID_TOKEN" : "UNAUTHORIZED");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                "You do not have permission to perform this action", "FORBIDDEN");
    }

    private void write(HttpServletResponse response, int status, String message, String errorCode)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message, errorCode));
    }
}
