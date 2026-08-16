package com.charitymanagement.api.charitymanagementback.volunteer;

import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Volunteer registry, task assignment, task status transitions and the activity feed. */
class VolunteerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A volunteer is registered with a generated VOL code")
    void registerVolunteer() throws Exception {
        mockMvc.perform(authPost("/api/volunteers", adminToken, Map.of(
                        "volunteerName", "Nimal Perera",
                        "phone", "0779998888",
                        "email", "nimal@volunteer.test",
                        "joinedDate", LocalDate.now().minusMonths(2).toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.volunteerCode").value("VOL-000001"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Registering a volunteer is ADMIN-only")
    void registrationIsAdminOnly() throws Exception {
        mockMvc.perform(authPost("/api/volunteers", staffToken, Map.of(
                        "volunteerName", "Nimal Perera")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Volunteers can be searched, filtered by status and updated")
    void searchFilterAndUpdate() throws Exception {
        long nimal = createVolunteer("Nimal Perera");
        long kamala = createVolunteer("Kamala Fernando");

        mockMvc.perform(authGet("/api/volunteers?search=kamala", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].volunteerName").value("Kamala Fernando"));

        mockMvc.perform(authPut("/api/volunteers/" + nimal, adminToken, Map.of(
                        "volunteerName", "Nimal Perera", "phone", "0711112222")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("0711112222"));

        mockMvc.perform(authPatch("/api/volunteers/" + kamala + "/status", adminToken,
                        Map.of("status", "INACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(authGet("/api/volunteers?status=ACTIVE", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].volunteerName").value("Nimal Perera"));
    }

    @Test
    @DisplayName("A task walks PENDING to IN_PROGRESS to COMPLETED and records the finish time")
    void taskStatusTransitions() throws Exception {
        long volunteer = createVolunteer("Nimal Perera");
        long taskId = createTask(volunteer, "Pack food parcels");

        mockMvc.perform(authGet("/api/volunteer-tasks/" + taskId, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.assignedBy.email").value(STAFF_EMAIL));

        mockMvc.perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                        Map.of("status", "IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.startedAt").exists());

        mockMvc.perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                        Map.of("status", "COMPLETED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").exists());
    }

    @Test
    @DisplayName("Cancelling a task requires a reason")
    void cancellationNeedsReason() throws Exception {
        long volunteer = createVolunteer("Nimal Perera");
        long taskId = createTask(volunteer, "Deliver parcels");

        mockMvc.perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                        Map.of("status", "CANCELLED")))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                        Map.of("status", "CANCELLED", "reason", "Delivery postponed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Delivery postponed"));
    }

    @Test
    @DisplayName("A completed task cannot be reopened")
    void completedTaskIsTerminal() throws Exception {
        long volunteer = createVolunteer("Nimal Perera");
        long taskId = createTask(volunteer, "Sort donations");
        perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                Map.of("status", "COMPLETED")), 200);

        mockMvc.perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                        Map.of("status", "IN_PROGRESS")))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Tasks are listed per volunteer and filterable by status")
    void taskListing() throws Exception {
        long nimal = createVolunteer("Nimal Perera");
        long kamala = createVolunteer("Kamala Fernando");
        long done = createTask(nimal, "Pack parcels");
        createTask(nimal, "Deliver parcels");
        createTask(kamala, "Register beneficiaries");
        perform(authPatch("/api/volunteer-tasks/" + done + "/status", staffToken,
                Map.of("status", "COMPLETED")), 200);

        mockMvc.perform(authGet("/api/volunteers/" + nimal + "/tasks", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(authGet("/api/volunteer-tasks?status=PENDING", staffToken))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(authGet("/api/volunteer-tasks?volunteerId=" + kamala, staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("The activity feed records assignment, start and completion in order")
    void activityFeed() throws Exception {
        long volunteer = createVolunteer("Nimal Perera");
        long taskId = createTask(volunteer, "Pack food parcels");
        perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                Map.of("status", "IN_PROGRESS")), 200);
        perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                Map.of("status", "COMPLETED")), 200);

        MvcResult result = perform(authGet("/api/volunteers/" + volunteer + "/activity", staffToken), 200);
        JsonNode activity = body(result).path("data");

        assertThat(activity).hasSize(3);
        assertThat(activity.findValuesAsText("activityType"))
                .containsExactlyInAnyOrder("TASK_ASSIGNED", "TASK_STARTED", "TASK_COMPLETED");
        activity.forEach(entry -> assertThat(entry.path("taskId").asLong()).isEqualTo(taskId));
    }

    @Test
    @DisplayName("A task cannot be assigned to an unknown volunteer")
    void unknownVolunteerRejected() throws Exception {
        mockMvc.perform(authPost("/api/volunteer-tasks", staffToken, Map.of(
                        "volunteerId", 9999,
                        "title", "Pack parcels",
                        "taskDate", LocalDate.now().toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("A task title is required")
    void titleRequired() throws Exception {
        long volunteer = createVolunteer("Nimal Perera");

        mockMvc.perform(authPost("/api/volunteer-tasks", staffToken, Map.of(
                        "volunteerId", volunteer, "title", "  ", "taskDate", LocalDate.now().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    private long createTask(long volunteerId, String title) {
        return id(perform(authPost("/api/volunteer-tasks", staffToken, Map.of(
                "volunteerId", volunteerId,
                "title", title,
                "description", title + " for this week's distribution round",
                "taskDate", LocalDate.now().toString(),
                "dueDate", LocalDate.now().plusDays(3).toString())), 201));
    }
}
