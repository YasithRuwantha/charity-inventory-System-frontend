package com.charitymanagement.api.charitymanagementback.auth;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the pre-existing authentication flow. These tests do not re-implement login — they call
 * the original {@code /api/v1/auth} endpoints and assert the behaviour that the rest of the system
 * now depends on.
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Test
    @DisplayName("The existing login endpoint still issues a usable JWT")
    void loginReturnsWorkingToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String token = body(result).path("token").asText();

        // The token from the untouched login flow must open the new business endpoints.
        mockMvc.perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Registration through the existing endpoint keeps working")
    void registerStillWorks() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "New Staff", "email", "new.staff@charity.test",
                                "password", PASSWORD, "role", "INVENTORY_STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertThat(userRepository.findByEmail("new.staff@charity.test")).isPresent();
    }

    @Test
    @DisplayName("Self-registration cannot create an ADMIN account")
    void registerRejectsAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Would Be Admin", "email", "self.admin@charity.test",
                                "password", PASSWORD, "role", "ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Administrator accounts cannot be created through registration"));

        assertThat(userRepository.findByEmail("self.admin@charity.test")).isEmpty();
    }

    @Test
    @DisplayName("A wrong password is rejected and no token is issued")
    void invalidCredentialsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", "wrong-password"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("A protected endpoint without a JWT returns 401")
    void missingTokenRejected() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("A malformed JWT returns 401")
    void invalidTokenRejected() throws Exception {
        mockMvc.perform(get("/api/inventory").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An expired JWT returns 401 rather than a 500")
    void expiredTokenRejected() throws Exception {
        mockMvc.perform(get("/api/inventory").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("A deactivated account cannot log in")
    void inactiveUserCannotLogIn() throws Exception {
        User suspended = ensureUser("suspended@charity.test", "Suspended", UserRole.INVENTORY_STAFF);
        suspended.setStatus(UserStatus.INACTIVE);
        userRepository.save(suspended);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "suspended@charity.test", "password", PASSWORD))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Role restrictions are enforced: a volunteer cannot reach staff-only endpoints")
    void roleRestrictionsEnforced() throws Exception {
        mockMvc.perform(authGet("/api/donors", volunteerToken)).andExpect(status().isForbidden());
        mockMvc.perform(authGet("/api/audit-logs", staffToken)).andExpect(status().isForbidden());
        mockMvc.perform(authGet("/api/audit-logs", adminToken)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("The public endpoints stay reachable without a token")
    void publicEndpointsRemainOpen() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("CONNECTED"));

        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("/api/users/me resolves the caller from the token, not from a parameter")
    void currentUserComesFromToken() throws Exception {
        mockMvc.perform(authGet("/api/users/me?userId=999", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(STAFF_EMAIL))
                .andExpect(jsonPath("$.data.role").value("INVENTORY_STAFF"));
    }

    @Test
    @DisplayName("An unmapped URL answers 404 rather than leaking a 500")
    void unknownEndpointReturnsNotFound() throws Exception {
        mockMvc.perform(authGet("/api/does-not-exist", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ENDPOINT_NOT_FOUND"));
    }

    /** Signs a token that expired an hour ago, using the same key the application trusts. */
    private String expiredToken() {
        Date past = new Date(System.currentTimeMillis() - 3_600_000);
        return Jwts.builder()
                .setSubject(ADMIN_EMAIL)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(past)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();
    }
}
