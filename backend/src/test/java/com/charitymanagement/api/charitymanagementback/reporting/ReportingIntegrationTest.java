package com.charitymanagement.api.charitymanagementback.reporting;

import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reports, CSV exports and the dashboard. Each figure asserted here is cross-checked against the
 * records the test itself created, which is what proves nothing is hard-coded.
 */
class ReportingIntegrationTest extends AbstractIntegrationTest {

    private long food;
    private long hygiene;
    private long rice;
    private long soap;
    private long beneficiary;

    /**
     * A small but complete data set: two categories, four items (one low, one expired, one expiring),
     * a donation, a completed distribution, a volunteer and two tasks.
     */
    @BeforeEach
    void seedBusinessData() {
        food = createCategory("Food");
        hygiene = createCategory("Hygiene");
        rice = createItem(food, "Rice", 100, 20);
        soap = createItem(hygiene, "Soap", 5, 20);
        createItem(food, "Old Milk", 40, 5, LocalDate.now().minusDays(3));
        createItem(food, "Fresh Bread", 30, 5, LocalDate.now().plusDays(5));

        long donor = createDonor("Jane Doe");
        perform(authPost("/api/donations", staffToken, Map.of(
                "donorId", donor,
                "donationDate", LocalDate.now().toString(),
                "items", List.of(Map.of("inventoryItemId", rice, "quantity", 50)))), 201);

        beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = approvedRequest(beneficiary, rice, 20);
        perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()), 200);

        long volunteer = createVolunteer("Nimal Perera");
        long taskId = id(perform(authPost("/api/volunteer-tasks", staffToken, Map.of(
                "volunteerId", volunteer, "title", "Pack parcels",
                "taskDate", LocalDate.now().toString())), 201));
        perform(authPatch("/api/volunteer-tasks/" + taskId + "/status", staffToken,
                Map.of("status", "COMPLETED")), 200);
        perform(authPost("/api/volunteer-tasks", staffToken, Map.of(
                "volunteerId", volunteer, "title", "Deliver parcels",
                "taskDate", LocalDate.now().toString())), 201);
    }

    @Test
    @DisplayName("The inventory report totals match the items on record")
    void inventoryReport() throws Exception {
        // Rice: 100 + 50 donated - 20 distributed = 130; plus soap 5, milk 40, bread 30 = 205.
        mockMvc.perform(authGet("/api/reports/inventory", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(4))
                .andExpect(jsonPath("$.data.totalQuantity").value(205))
                .andExpect(jsonPath("$.data.lowStockItems").value(1))
                .andExpect(jsonPath("$.data.outOfStockItems").value(0))
                .andExpect(jsonPath("$.data.expiredItems").value(1))
                .andExpect(jsonPath("$.data.categoryBreakdown").isArray());

        mockMvc.perform(authGet("/api/reports/inventory?categoryId=" + hygiene, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(5));
    }

    @Test
    @DisplayName("The donation report totals and date filter come from the database")
    void donationReport() throws Exception {
        mockMvc.perform(authGet("/api/reports/donations", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDonations").value(1))
                .andExpect(jsonPath("$.data.totalQuantityDonated").value(50));

        // A window that excludes today must report nothing.
        mockMvc.perform(authGet("/api/reports/donations?startDate=" + LocalDate.now().minusDays(10)
                        + "&endDate=" + LocalDate.now().minusDays(5), staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDonations").value(0));
    }

    @Test
    @DisplayName("The distribution report reflects the completed hand-over")
    void distributionReport() throws Exception {
        mockMvc.perform(authGet("/api/reports/distributions", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantityDistributed").value(20))
                .andExpect(jsonPath("$.data.statusBreakdown").isArray())
                .andExpect(jsonPath("$.data.priorityBreakdown").isArray());
    }

    @Test
    @DisplayName("The beneficiary report counts registrations by status and priority")
    void beneficiaryReport() throws Exception {
        mockMvc.perform(authGet("/api/reports/beneficiaries", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBeneficiaries").value(1))
                .andExpect(jsonPath("$.data.statusBreakdown").isArray())
                .andExpect(jsonPath("$.data.priorityBreakdown").isArray());
    }

    @Test
    @DisplayName("The volunteer report counts tasks by outcome")
    void volunteerReport() throws Exception {
        mockMvc.perform(authGet("/api/reports/volunteers", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVolunteers").value(1))
                .andExpect(jsonPath("$.data.activeVolunteers").value(1))
                .andExpect(jsonPath("$.data.totalTasks").value(2))
                .andExpect(jsonPath("$.data.completedTasks").value(1));
    }

    @Test
    @DisplayName("The expiry report separates expired stock from stock expiring soon")
    void expiryReport() throws Exception {
        mockMvc.perform(authGet("/api/reports/expiry", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expired.length()").value(1))
                .andExpect(jsonPath("$.data.expired[0].itemName").value("Old Milk"))
                .andExpect(jsonPath("$.data.expiringWithin7Days.length()").value(1))
                .andExpect(jsonPath("$.data.expiringWithin30Days.length()").value(1));
    }

    @Test
    @DisplayName("The stock movement report lists the whole ledger with per-type totals")
    void stockMovementReport() throws Exception {
        // 4 opening balances + 1 donation in + 1 distribution out.
        mockMvc.perform(authGet("/api/reports/stock-movements", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movements.length()").value(6));

        mockMvc.perform(authGet("/api/reports/stock-movements?transactionType=DISTRIBUTION_OUT", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movements.length()").value(1));

        mockMvc.perform(authGet("/api/reports/stock-movements?itemId=" + soap, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movements.length()").value(1));
    }

    @Test
    @DisplayName("Every report exports as CSV with the right headers")
    void csvExports() throws Exception {
        for (String report : List.of("inventory", "donations", "distributions", "beneficiaries",
                "volunteers", "stock-movements", "expiry")) {
            MvcResult result = mockMvc.perform(authGet("/api/reports/" + report + "/export", staffToken))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentType()).contains("text/csv");
            assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                    .contains("attachment", ".csv");
            assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).isNotBlank();
        }
    }

    @Test
    @DisplayName("The dashboard summary matches the seeded records exactly")
    void dashboardSummary() throws Exception {
        mockMvc.perform(authGet("/api/dashboard/summary", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInventoryItems").value(4))
                .andExpect(jsonPath("$.data.totalStockUnits").value(205))
                .andExpect(jsonPath("$.data.lowStockItems").value(1))
                .andExpect(jsonPath("$.data.outOfStockItems").value(0))
                .andExpect(jsonPath("$.data.expiredItems").value(1))
                .andExpect(jsonPath("$.data.expiringSoonItems").value(1))
                .andExpect(jsonPath("$.data.totalCategories").value(2))
                .andExpect(jsonPath("$.data.totalDonors").value(1))
                .andExpect(jsonPath("$.data.totalBeneficiaries").value(1))
                .andExpect(jsonPath("$.data.totalVolunteers").value(1))
                .andExpect(jsonPath("$.data.donationsThisMonth").value(1))
                .andExpect(jsonPath("$.data.distributionsThisMonth").value(1))
                .andExpect(jsonPath("$.data.pendingDistributionRequests").value(0))
                .andExpect(jsonPath("$.data.pendingVolunteerTasks").value(1))
                .andExpect(jsonPath("$.data.generatedAt").exists());
    }

    @Test
    @DisplayName("Dashboard counters move when the underlying data moves")
    void dashboardIsNotHardCoded() throws Exception {
        mockMvc.perform(authGet("/api/dashboard/summary", staffToken))
                .andExpect(jsonPath("$.data.totalDonors").value(1));

        createDonor("Second Donor");
        createDonor("Third Donor");

        mockMvc.perform(authGet("/api/dashboard/summary", staffToken))
                .andExpect(jsonPath("$.data.totalDonors").value(3));
    }

    @Test
    @DisplayName("Trends return one point per month for the requested window")
    void dashboardTrends() throws Exception {
        MvcResult donations = perform(authGet("/api/dashboard/donation-trend?period=6months", staffToken), 200);
        JsonNode points = body(donations).path("data");
        assertThat(points).hasSize(6);
        // The current month is last and carries the single donation of 50 units.
        JsonNode current = points.get(5);
        assertThat(current.path("count").asLong()).isEqualTo(1);
        assertThat(current.path("quantity").asLong()).isEqualTo(50);

        assertThat(body(perform(authGet("/api/dashboard/donation-trend?period=12months", staffToken), 200))
                .path("data")).hasSize(12);

        JsonNode distributions = body(perform(
                authGet("/api/dashboard/distribution-trend?period=6months", staffToken), 200)).path("data");
        assertThat(distributions).hasSize(6);
        assertThat(distributions.get(5).path("count").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("An unsupported trend period is rejected rather than silently defaulted")
    void invalidPeriodRejected() throws Exception {
        mockMvc.perform(authGet("/api/dashboard/donation-trend?period=99years", staffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Category and priority breakdowns add up to the stock and registrations held")
    void dashboardBreakdowns() throws Exception {
        MvcResult result = perform(authGet("/api/dashboard/inventory-by-category", staffToken), 200);
        JsonNode categories = body(result).path("data");
        assertThat(categories).hasSize(2);
        long totalQuantity = 0;
        for (JsonNode entry : categories) {
            totalQuantity += entry.path("quantity").asLong();
        }
        assertThat(totalQuantity).isEqualTo(205);

        mockMvc.perform(authGet("/api/dashboard/beneficiary-priority", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("HIGH"))
                .andExpect(jsonPath("$.data[0].count").value(1));
    }

    @Test
    @DisplayName("Alerts surface expired, low and expiring stock")
    void dashboardAlerts() throws Exception {
        MvcResult result = perform(authGet("/api/dashboard/alerts", staffToken), 200);
        List<String> types = new ArrayList<>();
        body(result).path("data").forEach(alert -> types.add(alert.path("type").asText()));

        assertThat(types).contains("EXPIRED_STOCK", "LOW_STOCK", "EXPIRING_SOON");
        assertThat(types).doesNotContain("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("The recent activity feed is populated from real audited actions")
    void recentActivity() throws Exception {
        MvcResult result = perform(authGet("/api/dashboard/recent-activity", staffToken), 200);
        JsonNode activity = body(result).path("data");

        assertThat(activity).isNotEmpty();
        assertThat(activity.get(0).path("occurredAt").asText()).isNotBlank();
        List<String> actions = new ArrayList<>();
        activity.forEach(entry -> actions.add(entry.path("action").asText()));
        assertThat(actions).contains("COMPLETE_DISTRIBUTION");
    }

    @Test
    @DisplayName("Reports and the dashboard are closed to volunteers")
    void reportingIsStaffOnly() throws Exception {
        mockMvc.perform(authGet("/api/reports/inventory", volunteerToken)).andExpect(status().isForbidden());
        mockMvc.perform(authGet("/api/dashboard/summary", volunteerToken)).andExpect(status().isForbidden());
    }

    private long approvedRequest(long beneficiaryId, long itemId, int quantity) {
        long requestId = id(perform(authPost("/api/distributions", staffToken, Map.of(
                "beneficiaryId", beneficiaryId,
                "requestDate", LocalDate.now().toString(),
                "items", List.of(Map.of("inventoryItemId", itemId, "requestedQuantity", quantity)))), 201));
        long lineId = body(perform(authGet("/api/distributions/" + requestId, staffToken), 200))
                .path("data").path("items").get(0).path("id").asLong();
        perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken, Map.of(
                "items", List.of(Map.of("distributionItemId", lineId, "allocatedQuantity", quantity)))), 200);
        perform(authPost("/api/distributions/" + requestId + "/approve", adminToken, Map.of()), 200);
        return requestId;
    }
}
