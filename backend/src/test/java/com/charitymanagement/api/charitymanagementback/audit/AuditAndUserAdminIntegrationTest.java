package com.charitymanagement.api.charitymanagementback.audit;

import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The audit trail and the account-administration endpoints, both ADMIN-only. */
class AuditAndUserAdminIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Business actions land in the audit trail with the acting user attached")
    void actionsAreAudited() throws Exception {
        long category = createCategory("Food");
        long item = createItem(category, "Rice", 100, 20);
        perform(authPost("/api/inventory/" + item + "/adjust-stock", staffToken,
                Map.of("adjustment", -10, "notes", "Spoilage")), 200);

        MvcResult result = perform(authGet("/api/audit-logs", adminToken), 200);
        JsonNode entries = body(result).path("data");
        assertThat(entries).isNotEmpty();
        assertThat(entries.findValuesAsText("action"))
                .contains("CREATE_CATEGORY", "CREATE_INVENTORY", "ADJUST_STOCK");

        JsonNode adjust = entries.get(0);
        assertThat(adjust.path("action").asText()).isEqualTo("ADJUST_STOCK");
        assertThat(adjust.path("userEmail").asText()).isEqualTo(STAFF_EMAIL);
        assertThat(adjust.path("entityType").asText()).isEqualTo("InventoryItem");
        assertThat(adjust.path("entityId").asText()).isEqualTo(String.valueOf(item));
        assertThat(adjust.path("createdAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("The audit trail filters by action, entity, user and date range")
    void auditFilters() throws Exception {
        long category = createCategory("Food");
        createItem(category, "Rice", 100, 20);

        mockMvc.perform(authGet("/api/audit-logs?action=CREATE_CATEGORY", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entityType").value("InventoryCategory"));

        mockMvc.perform(authGet("/api/audit-logs?entityType=InventoryItem", adminToken))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(authGet("/api/audit-logs?userId=" + staffId, adminToken))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(authGet("/api/audit-logs?userId=" + adminId, adminToken))
                .andExpect(jsonPath("$.data.length()").value(0));

        // A window that ends yesterday cannot contain anything created today.
        mockMvc.perform(authGet("/api/audit-logs?startDate=" + LocalDate.now().minusDays(5)
                        + "&endDate=" + LocalDate.now().minusDays(1), adminToken))
                .andExpect(jsonPath("$.data.length()").value(0));

        // Today's window is inclusive of the whole day.
        mockMvc.perform(authGet("/api/audit-logs?startDate=" + LocalDate.now()
                        + "&endDate=" + LocalDate.now(), adminToken))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("A start date after the end date is refused")
    void invertedDateRangeRejected() throws Exception {
        mockMvc.perform(authGet("/api/audit-logs?startDate=" + LocalDate.now()
                        + "&endDate=" + LocalDate.now().minusDays(3), adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Audit entries never contain credentials")
    void credentialsAreNeverAudited() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sneaky", "email", "sneaky@charity.test",
                                "password", "SuperSecret@123", "role", "VOLUNTEER"))))
                .andExpect(status().isOk());

        MvcResult result = perform(authGet("/api/audit-logs?size=100", adminToken), 200);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("SuperSecret@123", "$2a$");
    }

    @Test
    @DisplayName("The audit trail is paged and lists its available actions")
    void pagingAndActionList() throws Exception {
        long category = createCategory("Food");
        createItem(category, "Rice", 100, 20);
        createItem(category, "Milk", 50, 5);

        mockMvc.perform(authGet("/api/audit-logs?page=0&size=2", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.totalElements").value(3))
                .andExpect(jsonPath("$.pagination.totalPages").value(2));

        mockMvc.perform(authGet("/api/audit-logs/actions", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Only an admin can read the audit trail or administer users")
    void adminOnly() throws Exception {
        mockMvc.perform(authGet("/api/audit-logs", staffToken)).andExpect(status().isForbidden());
        mockMvc.perform(authGet("/api/audit-logs", volunteerToken)).andExpect(status().isForbidden());
        mockMvc.perform(authGet("/api/users", staffToken)).andExpect(status().isForbidden());
        mockMvc.perform(authGet("/api/users", adminToken)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Users can be listed, searched and filtered, and no hash is ever returned")
    void userListing() throws Exception {
        mockMvc.perform(authGet("/api/users", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].password").doesNotExist());

        mockMvc.perform(authGet("/api/users?role=VOLUNTEER", adminToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value(VOLUNTEER_EMAIL));

        mockMvc.perform(authGet("/api/users?search=staff", adminToken))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(authGet("/api/users?status=ACTIVE", adminToken))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("Changing a role takes effect and is audited")
    void changeRole() throws Exception {
        long volunteerId = userRepository.findByEmail(VOLUNTEER_EMAIL).orElseThrow().getId();

        mockMvc.perform(authPatch("/api/users/" + volunteerId + "/role", adminToken,
                        Map.of("role", "INVENTORY_STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("INVENTORY_STAFF"));

        assertThat(userRepository.findById(volunteerId).orElseThrow().getRole())
                .isEqualTo(UserRole.INVENTORY_STAFF);

        mockMvc.perform(authGet("/api/audit-logs?action=CHANGE_USER_ROLE", adminToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userEmail").value(ADMIN_EMAIL));
    }

    @Test
    @DisplayName("A deactivated user is locked out and the change is audited")
    void deactivateUser() throws Exception {
        long staffUserId = userRepository.findByEmail(STAFF_EMAIL).orElseThrow().getId();

        mockMvc.perform(authPatch("/api/users/" + staffUserId + "/status", adminToken,
                        Map.of("status", "INACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", STAFF_EMAIL, "password", PASSWORD))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(authGet("/api/audit-logs?action=USER_STATUS_CHANGE", adminToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("An admin cannot deactivate themselves")
    void selfDeactivationBlocked() throws Exception {
        mockMvc.perform(authPatch("/api/users/" + adminId + "/status", adminToken,
                        Map.of("status", "INACTIVE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SELF_DEACTIVATION_BLOCKED"));
    }

    @Test
    @DisplayName("The last active administrator can be neither demoted nor deactivated")
    void lastAdminIsProtected() throws Exception {
        // A second admin exists only to perform the change, so the target really is the last one.
        String otherAdminToken = tokenFor(ensureUser("admin2@charity.test", "Second Admin", UserRole.ADMIN));
        long otherAdminId = userRepository.findByEmail("admin2@charity.test").orElseThrow().getId();

        // With two admins the demotion is allowed.
        mockMvc.perform(authPatch("/api/users/" + adminId + "/role", otherAdminToken,
                        Map.of("role", "INVENTORY_STAFF")))
                .andExpect(status().isOk());

        // Now only one admin remains, so it is protected.
        mockMvc.perform(authPatch("/api/users/" + otherAdminId + "/role", otherAdminToken,
                        Map.of("role", "VOLUNTEER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LAST_ADMIN_PROTECTED"));
    }

    @Test
    @DisplayName("Role and status bodies are validated")
    void requestBodiesValidated() throws Exception {
        mockMvc.perform(authPatch("/api/users/" + adminId + "/role", adminToken, Map.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").exists());

        mockMvc.perform(authPatch("/api/users/" + adminId + "/role", adminToken,
                        Map.of("role", "SUPREME_LEADER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("An unknown user id returns 404")
    void unknownUser() throws Exception {
        mockMvc.perform(authGet("/api/users/9999", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
