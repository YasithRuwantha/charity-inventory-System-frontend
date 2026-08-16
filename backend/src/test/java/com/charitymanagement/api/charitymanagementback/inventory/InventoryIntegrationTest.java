package com.charitymanagement.api.charitymanagementback.inventory;

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

/** Categories, item lifecycle, status calculation and the stock ledger. */
class InventoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A category is created with a generated id and defaults to ACTIVE")
    void createCategory() throws Exception {
        mockMvc.perform(authPost("/api/categories", staffToken,
                        Map.of("name", "Food", "description", "Dry rations")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Food"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("A duplicate category name is refused with 409")
    void duplicateCategoryRejected() throws Exception {
        createCategory("Food");

        mockMvc.perform(authPost("/api/categories", staffToken, Map.of("name", "  food  ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("An item is created with a generated INV code and an opening ledger entry")
    void createItemGeneratesCodeAndLedgerEntry() throws Exception {
        long categoryId = createCategory("Food");

        MvcResult created = perform(authPost("/api/inventory", staffToken, Map.of(
                "itemName", "Rice",
                "categoryId", categoryId,
                "quantity", 100,
                "unit", "KG",
                "minimumStockLevel", 20)), 201);

        JsonNode item = body(created).path("data");
        assertThat(item.path("itemCode").asText()).isEqualTo("INV-000001");
        assertThat(item.path("quantity").asInt()).isEqualTo(100);
        assertThat(item.path("status").asText()).isEqualTo("IN_STOCK");
        assertThat(item.path("createdBy").path("email").asText()).isEqualTo(STAFF_EMAIL);

        // The opening balance is a stock movement like any other and must be on the ledger.
        JsonNode ledger = body(perform(authGet("/api/inventory/" + item.path("id").asLong()
                + "/transactions", staffToken), 200)).path("data");
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).path("quantityBefore").asInt()).isZero();
        assertThat(ledger.get(0).path("quantityAfter").asInt()).isEqualTo(100);
    }

    @Test
    @DisplayName("Reference codes stay unique and sequential across items")
    void itemCodesAreSequential() {
        long categoryId = createCategory("Food");
        long first = createItem(categoryId, "Rice", 10, 1);
        long second = createItem(categoryId, "Milk", 10, 1);

        String firstCode = body(perform(authGet("/api/inventory/" + first, staffToken), 200))
                .path("data").path("itemCode").asText();
        String secondCode = body(perform(authGet("/api/inventory/" + second, staffToken), 200))
                .path("data").path("itemCode").asText();

        assertThat(firstCode).isEqualTo("INV-000001");
        assertThat(secondCode).isEqualTo("INV-000002");
    }

    @Test
    @DisplayName("Updating an item cannot change its quantity")
    void updateDoesNotTouchQuantity() throws Exception {
        long categoryId = createCategory("Food");
        long itemId = createItem(categoryId, "Rice", 100, 20);

        mockMvc.perform(authPut("/api/inventory/" + itemId, staffToken, Map.of(
                        "itemName", "Basmati Rice",
                        "categoryId", categoryId,
                        "unit", "KG",
                        "minimumStockLevel", 25)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemName").value("Basmati Rice"))
                .andExpect(jsonPath("$.data.quantity").value(100));
    }

    @Test
    @DisplayName("A manual adjustment moves stock and writes a ledger row")
    void adjustStockWritesLedgerRow() throws Exception {
        long categoryId = createCategory("Food");
        long itemId = createItem(categoryId, "Rice", 100, 20);

        mockMvc.perform(authPost("/api/inventory/" + itemId + "/adjust-stock", staffToken,
                        Map.of("adjustment", -30, "notes", "Damaged in transit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(70));

        JsonNode ledger = body(perform(authGet("/api/inventory/" + itemId + "/transactions", staffToken), 200))
                .path("data");
        assertThat(ledger).hasSize(2);
        JsonNode adjustment = ledger.get(0);
        assertThat(adjustment.path("transactionType").asText()).isEqualTo("MANUAL_ADJUSTMENT");
        assertThat(adjustment.path("quantityBefore").asInt()).isEqualTo(100);
        assertThat(adjustment.path("quantityAfter").asInt()).isEqualTo(70);
        assertThat(adjustment.path("performedBy").path("email").asText()).isEqualTo(STAFF_EMAIL);
    }

    @Test
    @DisplayName("An adjustment that would drive stock negative is refused and changes nothing")
    void negativeStockRejected() throws Exception {
        long categoryId = createCategory("Food");
        long itemId = createItem(categoryId, "Rice", 10, 2);

        mockMvc.perform(authPost("/api/inventory/" + itemId + "/adjust-stock", staffToken,
                        Map.of("adjustment", -15)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));

        assertThat(quantityOf(itemId)).isEqualTo(10);
    }

    @Test
    @DisplayName("Status follows quantity: IN_STOCK, LOW_STOCK then OUT_OF_STOCK")
    void statusTracksQuantity() {
        long categoryId = createCategory("Food");
        long itemId = createItem(categoryId, "Rice", 100, 20);
        assertThat(statusOf(itemId)).isEqualTo("IN_STOCK");

        perform(authPost("/api/inventory/" + itemId + "/adjust-stock", staffToken,
                Map.of("adjustment", -85)), 200);
        assertThat(statusOf(itemId)).isEqualTo("LOW_STOCK");

        perform(authPost("/api/inventory/" + itemId + "/adjust-stock", staffToken,
                Map.of("adjustment", -15)), 200);
        assertThat(statusOf(itemId)).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("An item past its expiry date reports EXPIRED regardless of quantity")
    void expiredStatusWins() {
        long categoryId = createCategory("Medicine");
        long itemId = createItem(categoryId, "Paracetamol", 500, 10, LocalDate.now().minusDays(1));

        assertThat(statusOf(itemId)).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("The low-stock, out-of-stock, expired and expiring-soon views each return the right rows")
    void stockViews() throws Exception {
        long food = createCategory("Food");
        createItem(food, "Healthy Rice", 100, 20);
        createItem(food, "Low Lentils", 5, 20);
        createItem(food, "Empty Flour", 0, 10);
        createItem(food, "Old Milk", 40, 5, LocalDate.now().minusDays(2));
        createItem(food, "Soon Bread", 40, 5, LocalDate.now().plusDays(5));

        mockMvc.perform(authGet("/api/inventory/low-stock", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Low Lentils"));

        mockMvc.perform(authGet("/api/inventory/out-of-stock", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Empty Flour"));

        mockMvc.perform(authGet("/api/inventory/expired", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Old Milk"));

        mockMvc.perform(authGet("/api/inventory/expiring-soon?days=7", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Soon Bread"));
    }

    @Test
    @DisplayName("Search, category filter and pagination narrow the listing")
    void searchFilterAndPaging() throws Exception {
        long food = createCategory("Food");
        long hygiene = createCategory("Hygiene");
        createItem(food, "Rice", 50, 5);
        createItem(food, "Red Lentils", 50, 5);
        createItem(hygiene, "Soap", 50, 5);

        mockMvc.perform(authGet("/api/inventory?search=ric", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Rice"));

        mockMvc.perform(authGet("/api/inventory?categoryId=" + hygiene, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Soap"));

        mockMvc.perform(authGet("/api/inventory?page=0&size=2&sort=itemName", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.totalElements").value(3))
                .andExpect(jsonPath("$.pagination.totalPages").value(2));
    }

    @Test
    @DisplayName("A page size beyond the cap is clamped instead of loading the whole table")
    void pageSizeIsCapped() throws Exception {
        createCategory("Food");

        mockMvc.perform(authGet("/api/inventory?size=5000", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.size").value(100));
    }

    @Test
    @DisplayName("Deleting an item archives it and preserves its ledger")
    void deleteArchivesInsteadOfRemoving() throws Exception {
        long categoryId = createCategory("Food");
        long itemId = createItem(categoryId, "Rice", 100, 20);

        mockMvc.perform(authDelete("/api/inventory/" + itemId, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived").value(true));

        // Still retrievable by id, and its history survived.
        mockMvc.perform(authGet("/api/inventory/" + itemId, staffToken)).andExpect(status().isOk());
        mockMvc.perform(authGet("/api/inventory/" + itemId + "/transactions", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
        // Excluded from the default listing.
        mockMvc.perform(authGet("/api/inventory", staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Creating an item with a negative quantity fails validation")
    void validationRejectsNegativeOpeningStock() throws Exception {
        long categoryId = createCategory("Food");

        mockMvc.perform(authPost("/api/inventory", staffToken, Map.of(
                        "itemName", "Rice", "categoryId", categoryId, "quantity", -5,
                        "unit", "KG", "minimumStockLevel", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    @DisplayName("The combined ledger endpoint filters by item and reference type")
    void ledgerSearch() throws Exception {
        long categoryId = createCategory("Food");
        long rice = createItem(categoryId, "Rice", 100, 20);
        createItem(categoryId, "Milk", 50, 5);
        perform(authPost("/api/inventory/" + rice + "/adjust-stock", staffToken,
                Map.of("adjustment", 20)), 200);

        // Rice has its opening balance plus the adjustment; milk only has its opening balance.
        mockMvc.perform(authGet("/api/inventory-transactions?itemId=" + rice, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Opening balances are booked against SYSTEM, so only the explicit adjustment is MANUAL.
        mockMvc.perform(authGet("/api/inventory-transactions?referenceType=MANUAL", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(authGet("/api/inventory-transactions", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }
}
