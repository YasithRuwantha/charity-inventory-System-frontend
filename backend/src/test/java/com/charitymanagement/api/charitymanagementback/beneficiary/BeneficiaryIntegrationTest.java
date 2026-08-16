package com.charitymanagement.api.charitymanagementback.beneficiary;

import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Beneficiary registry: family information, priority, search and activation. */
class BeneficiaryIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A beneficiary is registered with a generated BEN code and their family details")
    void registerBeneficiary() throws Exception {
        mockMvc.perform(authPost("/api/beneficiaries", staffToken, Map.of(
                        "beneficiaryName", "Ayesha Khan",
                        "identificationNumber", "NIC-9912345",
                        "familySize", 4,
                        "contactNumber", "0771234567",
                        "address", "12 Temple Road",
                        "priorityLevel", "HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.beneficiaryCode").value("BEN-000001"))
                .andExpect(jsonPath("$.data.familySize").value(4))
                .andExpect(jsonPath("$.data.priorityLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("A family size below 1 is rejected")
    void familySizeValidated() throws Exception {
        mockMvc.perform(authPost("/api/beneficiaries", staffToken, Map.of(
                        "beneficiaryName", "Ayesha Khan", "familySize", 0, "priorityLevel", "LOW")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.familySize").exists());
    }

    @Test
    @DisplayName("A repeated identification number is refused")
    void duplicateIdentificationRejected() throws Exception {
        Map<String, Object> payload = Map.of("beneficiaryName", "Ayesha Khan",
                "identificationNumber", "NIC-9912345", "familySize", 3, "priorityLevel", "MEDIUM");
        perform(authPost("/api/beneficiaries", staffToken, payload), 201);

        mockMvc.perform(authPost("/api/beneficiaries", staffToken, Map.of(
                        "beneficiaryName", "Someone Else",
                        "identificationNumber", "NIC-9912345",
                        "familySize", 2, "priorityLevel", "LOW")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Two beneficiaries without an identification number are both accepted")
    void identificationNumberIsOptional() throws Exception {
        createBeneficiary("First Family", 3, "LOW");

        mockMvc.perform(authPost("/api/beneficiaries", staffToken, Map.of(
                        "beneficiaryName", "Second Family", "familySize", 2, "priorityLevel", "LOW")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.beneficiaryCode").value("BEN-000002"));
    }

    @Test
    @DisplayName("Search covers code, name, identification number and contact number")
    void searchAcrossIdentifiers() throws Exception {
        perform(authPost("/api/beneficiaries", staffToken, Map.of(
                "beneficiaryName", "Ayesha Khan", "identificationNumber", "NIC-9912345",
                "contactNumber", "0771234567", "familySize", 4, "priorityLevel", "HIGH")), 201);
        createBeneficiary("Bandara Silva", 2, "LOW");

        mockMvc.perform(authGet("/api/beneficiaries?search=ayesha", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(authGet("/api/beneficiaries?search=NIC-99", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(authGet("/api/beneficiaries?search=077123", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(authGet("/api/beneficiaries?search=BEN-000002", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].beneficiaryName").value("Bandara Silva"));
    }

    @Test
    @DisplayName("Priority and status filters narrow the listing, and paging is reported")
    void filtersAndPaging() throws Exception {
        createBeneficiary("High Family", 5, "HIGH");
        createBeneficiary("Low Family", 1, "LOW");
        long inactive = createBeneficiary("Former Family", 2, "MEDIUM");
        perform(authPatch("/api/beneficiaries/" + inactive + "/status", staffToken,
                Map.of("status", "INACTIVE")), 200);

        mockMvc.perform(authGet("/api/beneficiaries?priority=HIGH", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].beneficiaryName").value("High Family"));

        mockMvc.perform(authGet("/api/beneficiaries?status=INACTIVE", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].beneficiaryName").value("Former Family"));

        mockMvc.perform(authGet("/api/beneficiaries?page=0&size=2", staffToken))
                .andExpect(jsonPath("$.pagination.totalElements").value(3))
                .andExpect(jsonPath("$.pagination.totalPages").value(2));
    }

    @Test
    @DisplayName("A beneficiary can be edited and deactivated without losing their record")
    void updateAndDeactivate() throws Exception {
        long id = createBeneficiary("Ayesha Khan", 4, "HIGH");

        mockMvc.perform(authPut("/api/beneficiaries/" + id, staffToken, Map.of(
                        "beneficiaryName", "Ayesha Khan",
                        "familySize", 6,
                        "priorityLevel", "MEDIUM",
                        "address", "New address")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.familySize").value(6))
                .andExpect(jsonPath("$.data.priorityLevel").value("MEDIUM"));

        mockMvc.perform(authPatch("/api/beneficiaries/" + id + "/status", staffToken,
                        Map.of("status", "INACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(authGet("/api/beneficiaries/" + id, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.beneficiaryCode").value("BEN-000001"));
    }

    @Test
    @DisplayName("An unknown beneficiary returns 404 rather than an empty object")
    void unknownBeneficiaryNotFound() throws Exception {
        mockMvc.perform(authGet("/api/beneficiaries/9999", staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Statistics for a beneficiary with no aid yet report zeroes, not nulls")
    void statisticsStartAtZero() throws Exception {
        long id = createBeneficiary("Ayesha Khan", 4, "HIGH");

        mockMvc.perform(authGet("/api/beneficiaries/" + id + "/statistics", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDistributions").value(0))
                .andExpect(jsonPath("$.data.totalQuantityReceived").value(0))
                .andExpect(jsonPath("$.data.pendingRequests").value(0))
                .andExpect(jsonPath("$.data.priorityLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.familySize").value(4));

        mockMvc.perform(authGet("/api/beneficiaries/" + id + "/distributions", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
