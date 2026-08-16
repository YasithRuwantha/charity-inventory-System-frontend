package com.charitymanagement.api.charitymanagementback.donation;

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

/** Donors, multi-item donations, the resulting stock increase, receipts and donation history. */
class DonationIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A donor is registered with a generated DONOR code")
    void registerDonor() throws Exception {
        mockMvc.perform(authPost("/api/donors", staffToken, Map.of(
                        "donorName", "Jane Doe", "donorType", "INDIVIDUAL", "email", "jane@donor.test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.donorCode").value("DONOR-000001"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("An invalid donor email fails validation")
    void donorEmailValidated() throws Exception {
        mockMvc.perform(authPost("/api/donors", staffToken, Map.of(
                        "donorName", "Jane Doe", "donorType", "INDIVIDUAL", "email", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("A multi-item donation raises stock and writes DONATION_IN ledger rows")
    void multiItemDonationRaisesStock() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long milk = createItem(category, "Milk", 30, 5);
        long donor = createDonor("Jane Doe");

        MvcResult created = perform(authPost("/api/donations", staffToken, Map.of(
                "donorId", donor,
                "donationDate", LocalDate.now().toString(),
                "notes", "Monthly food donation",
                "items", List.of(
                        Map.of("inventoryItemId", rice, "quantity", 50,
                                "expiryDate", LocalDate.now().plusMonths(6).toString()),
                        Map.of("inventoryItemId", milk, "quantity", 20)))), 201);

        JsonNode donation = body(created).path("data");
        assertThat(donation.path("donationReference").asText())
                .isEqualTo("DON-" + LocalDate.now().getYear() + "-000001");
        assertThat(donation.path("totalItems").asInt()).isEqualTo(2);
        assertThat(donation.path("totalQuantity").asLong()).isEqualTo(70);
        assertThat(donation.path("receivedBy").path("email").asText()).isEqualTo(STAFF_EMAIL);

        assertThat(quantityOf(rice)).isEqualTo(150);
        assertThat(quantityOf(milk)).isEqualTo(50);

        // Exactly the movement described in the end-to-end scenario: 100 -> +50 -> 150.
        JsonNode ledger = body(perform(authGet("/api/inventory/" + rice + "/transactions", staffToken), 200))
                .path("data");
        JsonNode donationIn = ledger.get(0);
        assertThat(donationIn.path("transactionType").asText()).isEqualTo("DONATION_IN");
        assertThat(donationIn.path("referenceType").asText()).isEqualTo("DONATION");
        assertThat(donationIn.path("quantityBefore").asInt()).isEqualTo(100);
        assertThat(donationIn.path("quantity").asInt()).isEqualTo(50);
        assertThat(donationIn.path("quantityAfter").asInt()).isEqualTo(150);
        assertThat(donationIn.path("referenceId").asLong()).isEqualTo(donation.path("id").asLong());
    }

    @Test
    @DisplayName("An unknown donor is rejected and nothing is written")
    void unknownDonorRejected() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);

        mockMvc.perform(authPost("/api/donations", staffToken, Map.of(
                        "donorId", 9999,
                        "donationDate", LocalDate.now().toString(),
                        "items", List.of(Map.of("inventoryItemId", rice, "quantity", 10)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        assertThat(quantityOf(rice)).isEqualTo(100);
        mockMvc.perform(authGet("/api/donations", staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("An unknown inventory item rolls the whole donation back")
    void unknownItemRollsBackEntireDonation() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long donor = createDonor("Jane Doe");

        mockMvc.perform(authPost("/api/donations", staffToken, Map.of(
                        "donorId", donor,
                        "donationDate", LocalDate.now().toString(),
                        "items", List.of(
                                Map.of("inventoryItemId", rice, "quantity", 25),
                                Map.of("inventoryItemId", 9999, "quantity", 10)))))
                .andExpect(status().isNotFound());

        // The valid first line must not survive the failure of the second.
        assertThat(quantityOf(rice)).isEqualTo(100);
        mockMvc.perform(authGet("/api/donations", staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(authGet("/api/inventory/" + rice + "/transactions", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("A non-positive quantity and an empty item list both fail validation")
    void quantityAndItemListValidated() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long donor = createDonor("Jane Doe");

        mockMvc.perform(authPost("/api/donations", staffToken, Map.of(
                        "donorId", donor,
                        "donationDate", LocalDate.now().toString(),
                        "items", List.of(Map.of("inventoryItemId", rice, "quantity", 0)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(authPost("/api/donations", staffToken, Map.of(
                        "donorId", donor,
                        "donationDate", LocalDate.now().toString(),
                        "items", List.of())))
                .andExpect(status().isBadRequest());

        assertThat(quantityOf(rice)).isEqualTo(100);
    }

    @Test
    @DisplayName("A future donation date is rejected")
    void futureDonationDateRejected() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long donor = createDonor("Jane Doe");

        mockMvc.perform(authPost("/api/donations", staffToken, Map.of(
                        "donorId", donor,
                        "donationDate", LocalDate.now().plusDays(1).toString(),
                        "items", List.of(Map.of("inventoryItemId", rice, "quantity", 5)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.donationDate").exists());
    }

    @Test
    @DisplayName("Donation history is available globally, by reference and per donor — with item detail")
    void donationHistory() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long donor = createDonor("Jane Doe");
        registerDonation(donor, rice, 50);
        registerDonation(donor, rice, 10);

        mockMvc.perform(authGet("/api/donations", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].items").isArray())
                .andExpect(jsonPath("$.data[0].items[0].itemName").value("Rice"));

        String reference = "DON-" + LocalDate.now().getYear() + "-000001";
        mockMvc.perform(authGet("/api/donations/reference/" + reference, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.donationReference").value(reference));

        mockMvc.perform(authGet("/api/donors/" + donor + "/donations", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(authGet("/api/donors/" + donor + "/statistics", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDonations").value(2))
                .andExpect(jsonPath("$.data.totalQuantityDonated").value(60))
                .andExpect(jsonPath("$.data.lastDonationDate").value(LocalDate.now().toString()));
    }

    @Test
    @DisplayName("Donation search filters by donor and date range")
    void donationFilters() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 500, 20);
        long jane = createDonor("Jane Doe");
        long acme = createDonor("Acme Foundation");
        registerDonation(jane, rice, 10);
        registerDonation(acme, rice, 20);

        mockMvc.perform(authGet("/api/donations?donorId=" + acme, staffToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].donorName").value("Acme Foundation"));

        mockMvc.perform(authGet("/api/donations?startDate=" + LocalDate.now().plusDays(1)
                        + "&endDate=" + LocalDate.now().plusDays(2), staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(authGet("/api/donations?search=acme", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("The receipt carries everything a printed slip needs")
    void receiptContainsFullDetail() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long donor = createDonor("Jane Doe");
        long donationId = registerDonation(donor, rice, 50);

        mockMvc.perform(authGet("/api/donations/" + donationId + "/receipt", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("Test Charity"))
                .andExpect(jsonPath("$.data.donationReference").exists())
                .andExpect(jsonPath("$.data.donorCode").value("DONOR-000001"))
                .andExpect(jsonPath("$.data.donorName").value("Jane Doe"))
                .andExpect(jsonPath("$.data.receivedByName").value("Inventory Staff"))
                .andExpect(jsonPath("$.data.generatedAt").exists())
                .andExpect(jsonPath("$.data.items[0].itemName").value("Rice"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(50));
    }

    @Test
    @DisplayName("The donation items endpoint returns the lines on their own")
    void donationItemsEndpoint() throws Exception {
        long category = createCategory("Food");
        long rice = createItem(category, "Rice", 100, 20);
        long donor = createDonor("Jane Doe");
        long donationId = registerDonation(donor, rice, 50);

        mockMvc.perform(authGet("/api/donations/" + donationId + "/items", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].quantity").value(50));
    }

    /** Creates a single-line donation and returns its id. */
    private long registerDonation(long donorId, long itemId, int quantity) {
        return id(perform(authPost("/api/donations", staffToken, Map.of(
                "donorId", donorId,
                "donationDate", LocalDate.now().toString(),
                "items", List.of(Map.of("inventoryItemId", itemId, "quantity", quantity)))), 201));
    }
}
