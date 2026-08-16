package com.charitymanagement.api.charitymanagementback.distribution;

import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The distribution lifecycle. The rule under test throughout is that stock moves exactly once, at
 * completion — never on creation, allocation or approval — and that a failing line rolls the whole
 * hand-over back.
 */
class DistributionIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Full lifecycle: stock stays put until completion, then drops exactly once")
    void fullLifecycle() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 150, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        long requestId = createRequest(beneficiary, Map.of(rice, 20));

        JsonNode created = body(perform(authGet("/api/distributions/" + requestId, staffToken), 200))
                .path("data");
        assertThat(created.path("requestReference").asText())
                .isEqualTo("DIST-" + LocalDate.now().getYear() + "-000001");
        assertThat(created.path("status").asText()).isEqualTo("PENDING");
        assertThat(quantityOf(rice)).isEqualTo(150);

        long lineId = created.path("items").get(0).path("id").asLong();

        // Allocation is a planning step: it must not move stock.
        mockMvc.perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken, Map.of(
                        "items", List.of(Map.of("distributionItemId", lineId, "allocatedQuantity", 20)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].allocatedQuantity").value(20))
                .andExpect(jsonPath("$.data.items[0].requestedQuantity").value(20))
                .andExpect(jsonPath("$.data.items[0].availableQuantity").value(150));
        assertThat(quantityOf(rice)).isEqualTo(150);

        // Nor must approval.
        mockMvc.perform(authPost("/api/distributions/" + requestId + "/approve", adminToken, Map.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy.email").value(ADMIN_EMAIL));
        assertThat(quantityOf(rice)).isEqualTo(150);

        // Completion is the one and only stock movement.
        mockMvc.perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedBy.email").value(STAFF_EMAIL))
                .andExpect(jsonPath("$.data.items[0].distributedQuantity").value(20));
        assertThat(quantityOf(rice)).isEqualTo(130);

        JsonNode ledger = body(perform(authGet("/api/inventory/" + rice + "/transactions", staffToken), 200))
                .path("data");
        JsonNode movement = ledger.get(0);
        assertThat(movement.path("transactionType").asText()).isEqualTo("DISTRIBUTION_OUT");
        assertThat(movement.path("referenceType").asText()).isEqualTo("DISTRIBUTION");
        assertThat(movement.path("quantityBefore").asInt()).isEqualTo(150);
        // The ledger stores signed movements, so an outward move of 20 is recorded as -20.
        assertThat(movement.path("quantity").asInt()).isEqualTo(-20);
        assertThat(movement.path("quantityAfter").asInt()).isEqualTo(130);
        assertThat(movement.path("referenceId").asLong()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("A request for more than is in stock is refused with 409 and leaves stock alone")
    void insufficientStockRejected() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 130, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = createRequest(beneficiary, Map.of(rice, 200));
        long lineId = lineIds(requestId).get(0);

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken, Map.of(
                        "items", List.of(Map.of("distributionItemId", lineId, "allocatedQuantity", 200)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));

        assertThat(quantityOf(rice)).isEqualTo(130);
    }

    @Test
    @DisplayName("Allocating more than was requested is refused")
    void cannotAllocateMoreThanRequested() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 500, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = createRequest(beneficiary, Map.of(rice, 20));
        long lineId = lineIds(requestId).get(0);

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken, Map.of(
                        "items", List.of(Map.of("distributionItemId", lineId, "allocatedQuantity", 50)))))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Expired stock cannot be distributed and its quantity is untouched")
    void expiredStockCannotBeDistributed() throws Exception {
        long food = createCategory("Food");
        long expiredMilk = createItem(food, "Milk", 100, 10, LocalDate.now().minusDays(1));
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = createRequest(beneficiary, Map.of(expiredMilk, 10));
        long lineId = lineIds(requestId).get(0);

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken, Map.of(
                        "items", List.of(Map.of("distributionItemId", lineId, "allocatedQuantity", 10)))))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("EXPIRED_INVENTORY"));

        assertThat(quantityOf(expiredMilk)).isEqualTo(100);
        // The expired row itself is retained for audit purposes.
        mockMvc.perform(authGet("/api/inventory/" + expiredMilk, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));
    }

    @Test
    @DisplayName("A completed distribution cannot be completed a second time")
    void cannotCompleteTwice() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 100, 10);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = approvedRequest(beneficiary, Map.of(rice, 20));

        perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()), 200);
        assertThat(quantityOf(rice)).isEqualTo(80);

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISTRIBUTION_ALREADY_COMPLETED"));

        // No second deduction.
        assertThat(quantityOf(rice)).isEqualTo(80);
        mockMvc.perform(authGet("/api/inventory-transactions?transactionType=DISTRIBUTION_OUT", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("One failing line rolls the entire multi-item completion back")
    void multiItemRollback() throws Exception {
        long food = createCategory("Food");
        long medical = createCategory("Medicine");
        long rice = createItem(food, "Rice", 100, 10);
        long milk = createItem(food, "Milk", 50, 5);
        long medicine = createItem(medical, "Medicine", 20, 5);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        long requestId = approvedRequest(beneficiary, Map.of(rice, 10, milk, 10, medicine, 5));

        // Drain the medicine after approval, so completion finds it unavailable.
        perform(authPost("/api/inventory/" + medicine + "/adjust-stock", staffToken,
                Map.of("adjustment", -20, "notes", "Written off")), 200);
        assertThat(quantityOf(medicine)).isZero();

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));

        // Nothing partial survives: the two available items were not deducted.
        assertThat(quantityOf(rice)).isEqualTo(100);
        assertThat(quantityOf(milk)).isEqualTo(50);
        assertThat(quantityOf(medicine)).isZero();
        mockMvc.perform(authGet("/api/inventory-transactions?transactionType=DISTRIBUTION_OUT", staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(authGet("/api/distributions/" + requestId, staffToken))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("A repeat of the same item to the same beneficiary raises a duplicate warning")
    void duplicateDistributionDetected() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 500, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        long first = approvedRequest(beneficiary, Map.of(rice, 20));
        perform(authPost("/api/distributions/" + first + "/complete", staffToken, Map.of()), 200);
        assertThat(quantityOf(rice)).isEqualTo(480);

        MvcResult check = perform(authGet("/api/distributions/check-duplicate?beneficiaryId=" + beneficiary
                + "&inventoryItemId=" + rice, staffToken), 200);
        JsonNode duplicate = body(check).path("data");
        assertThat(duplicate.path("duplicateWarning").asBoolean()).isTrue();
        assertThat(duplicate.path("daysSinceLastDistribution").asInt()).isZero();
        assertThat(duplicate.path("previousDistributionId").asLong()).isEqualTo(first);
        assertThat(duplicate.path("quantityPreviouslyReceived").asInt()).isEqualTo(20);
        assertThat(duplicate.path("duplicateWindowDays").asInt()).isEqualTo(30);

        // A second hand-over of the same item is blocked rather than silently completed.
        long second = approvedRequest(beneficiary, Map.of(rice, 15));
        mockMvc.perform(authPost("/api/distributions/" + second + "/complete", staffToken, Map.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_DISTRIBUTION"));

        assertThat(quantityOf(rice)).isEqualTo(480);
    }

    @Test
    @DisplayName("An unrelated item to the same beneficiary is not treated as a duplicate")
    void differentItemIsNotADuplicate() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 200, 20);
        long milk = createItem(food, "Milk", 200, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        long first = approvedRequest(beneficiary, Map.of(rice, 20));
        perform(authPost("/api/distributions/" + first + "/complete", staffToken, Map.of()), 200);

        long second = approvedRequest(beneficiary, Map.of(milk, 10));
        mockMvc.perform(authPost("/api/distributions/" + second + "/complete", staffToken, Map.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertThat(quantityOf(milk)).isEqualTo(190);
    }

    @Test
    @DisplayName("Only an ADMIN may override a duplicate, and only with a reason")
    void duplicateOverrideRules() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 500, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        long first = approvedRequest(beneficiary, Map.of(rice, 20));
        perform(authPost("/api/distributions/" + first + "/complete", staffToken, Map.of()), 200);

        long second = approvedRequest(beneficiary, Map.of(rice, 15));

        // Staff cannot override.
        mockMvc.perform(authPost("/api/distributions/" + second + "/complete", staffToken,
                        Map.of("overrideDuplicates", true, "overrideReason", "Emergency assistance approved.")))
                .andExpect(status().isForbidden());
        assertThat(quantityOf(rice)).isEqualTo(480);

        // An admin without a reason cannot either.
        mockMvc.perform(authPost("/api/distributions/" + second + "/complete", adminToken,
                        Map.of("overrideDuplicates", true)))
                .andExpect(status().is4xxClientError());
        assertThat(quantityOf(rice)).isEqualTo(480);

        // An admin with a reason succeeds, and the override is recorded.
        mockMvc.perform(authPost("/api/distributions/" + second + "/complete", adminToken,
                        Map.of("overrideDuplicates", true,
                                "overrideReason", "Emergency assistance approved due to flooding.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        assertThat(quantityOf(rice)).isEqualTo(465);

        mockMvc.perform(authGet("/api/distributions/" + second + "/report", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overrides.length()").value(1))
                .andExpect(jsonPath("$.data.overrides[0].overrideReason")
                        .value("Emergency assistance approved due to flooding."))
                .andExpect(jsonPath("$.data.overrides[0].overriddenBy.email").value(ADMIN_EMAIL));

        mockMvc.perform(authGet("/api/audit-logs?action=OVERRIDE_DUPLICATE", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("Rejection and cancellation both require a reason and leave stock alone")
    void rejectAndCancel() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 100, 10);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        long toReject = createRequest(beneficiary, Map.of(rice, 10));
        mockMvc.perform(authPost("/api/distributions/" + toReject + "/reject", adminToken, Map.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        mockMvc.perform(authPost("/api/distributions/" + toReject + "/reject", adminToken,
                        Map.of("reason", "Beneficiary already served by a partner agency")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectedBy.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.rejectionReason").exists());

        long toCancel = createRequest(beneficiary, Map.of(rice, 10));
        mockMvc.perform(authPost("/api/distributions/" + toCancel + "/cancel", staffToken,
                        Map.of("reason", "Requested in error")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(quantityOf(rice)).isEqualTo(100);
    }

    @Test
    @DisplayName("A rejected request cannot then be completed")
    void rejectedRequestCannotComplete() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 100, 10);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = createRequest(beneficiary, Map.of(rice, 10));

        perform(authPost("/api/distributions/" + requestId + "/reject", adminToken,
                Map.of("reason", "Not eligible")), 200);

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(quantityOf(rice)).isEqualTo(100);
    }

    @Test
    @DisplayName("An inactive beneficiary cannot be given a new request")
    void inactiveBeneficiaryRejected() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 100, 10);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        perform(authPatch("/api/beneficiaries/" + beneficiary + "/status", staffToken,
                Map.of("status", "INACTIVE")), 200);

        mockMvc.perform(authPost("/api/distributions", staffToken, Map.of(
                        "beneficiaryId", beneficiary,
                        "requestDate", LocalDate.now().toString(),
                        "items", List.of(Map.of("inventoryItemId", rice, "requestedQuantity", 10)))))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("A request must contain at least one item")
    void emptyItemListRejected() throws Exception {
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");

        mockMvc.perform(authPost("/api/distributions", staffToken, Map.of(
                        "beneficiaryId", beneficiary,
                        "requestDate", LocalDate.now().toString(),
                        "items", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Non-admins cannot approve, and staff can still create and complete")
    void approvalIsAdminOnly() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 100, 10);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = createRequest(beneficiary, Map.of(rice, 10));
        long lineId = lineIds(requestId).get(0);
        perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken, Map.of(
                "items", List.of(Map.of("distributionItemId", lineId, "allocatedQuantity", 10)))), 200);

        mockMvc.perform(authPost("/api/distributions/" + requestId + "/approve", staffToken, Map.of()))
                .andExpect(status().isForbidden());
        mockMvc.perform(authPost("/api/distributions/" + requestId + "/approve", volunteerToken, Map.of()))
                .andExpect(status().isForbidden());
        mockMvc.perform(authPost("/api/distributions/" + requestId + "/approve", adminToken, Map.of()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The beneficiary's history and statistics reflect a completed hand-over")
    void beneficiaryHistoryAfterCompletion() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 150, 20);
        long beneficiary = createBeneficiary("Ayesha Khan", 4, "HIGH");
        long requestId = approvedRequest(beneficiary, Map.of(rice, 20));
        perform(authPost("/api/distributions/" + requestId + "/complete", staffToken, Map.of()), 200);

        mockMvc.perform(authGet("/api/distributions/beneficiary/" + beneficiary, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));

        mockMvc.perform(authGet("/api/beneficiaries/" + beneficiary + "/statistics", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDistributions").value(1))
                .andExpect(jsonPath("$.data.totalQuantityReceived").value(20))
                .andExpect(jsonPath("$.data.lastDistributionDate").exists());
    }

    @Test
    @DisplayName("Status and priority filters narrow the distribution listing")
    void searchAndFilters() throws Exception {
        long food = createCategory("Food");
        long rice = createItem(food, "Rice", 500, 20);
        long high = createBeneficiary("High Family", 5, "HIGH");
        long low = createBeneficiary("Low Family", 2, "LOW");
        createRequest(high, Map.of(rice, 10));
        long cancelled = createRequest(low, Map.of(rice, 10));
        perform(authPost("/api/distributions/" + cancelled + "/cancel", staffToken,
                Map.of("reason", "Duplicate entry")), 200);

        mockMvc.perform(authGet("/api/distributions?status=PENDING", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(authGet("/api/distributions?status=CANCELLED", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(authGet("/api/distributions?priority=HIGH", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(authGet("/api/distributions?beneficiaryId=" + low, staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Creates a PENDING request for the given item -> requested quantity pairs. */
    private long createRequest(long beneficiaryId, Map<Long, Integer> items) {
        List<Map<String, Object>> lines = items.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "inventoryItemId", entry.getKey(), "requestedQuantity", entry.getValue()))
                .toList();
        return id(perform(authPost("/api/distributions", staffToken, Map.of(
                "beneficiaryId", beneficiaryId,
                "requestDate", LocalDate.now().toString(),
                "reason", "Household support",
                "items", lines)), 201));
    }

    /** Creates a request, allocates each line in full and approves it. */
    private long approvedRequest(long beneficiaryId, Map<Long, Integer> items) {
        long requestId = createRequest(beneficiaryId, items);
        JsonNode lines = body(perform(authGet("/api/distributions/" + requestId, staffToken), 200))
                .path("data").path("items");

        List<Map<String, Object>> allocations = new java.util.ArrayList<>();
        lines.forEach(line -> allocations.add(Map.of(
                "distributionItemId", line.path("id").asLong(),
                "allocatedQuantity", line.path("requestedQuantity").asInt())));

        perform(authPost("/api/distributions/" + requestId + "/allocate", staffToken,
                Map.of("items", allocations)), 200);
        perform(authPost("/api/distributions/" + requestId + "/approve", adminToken, Map.of()), 200);
        return requestId;
    }

    private List<Long> lineIds(long requestId) {
        JsonNode lines = body(perform(authGet("/api/distributions/" + requestId, staffToken), 200))
                .path("data").path("items");
        List<Long> ids = new java.util.ArrayList<>();
        lines.forEach(line -> ids.add(line.path("id").asLong()));
        return ids;
    }
}
