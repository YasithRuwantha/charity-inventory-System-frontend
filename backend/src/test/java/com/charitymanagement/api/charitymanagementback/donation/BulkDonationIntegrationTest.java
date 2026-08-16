package com.charitymanagement.api.charitymanagementback.donation;

import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bulk donation upload: the preview writes nothing, and the import is all-or-nothing by default. */
class BulkDonationIntegrationTest extends AbstractIntegrationTest {

    private static final String HEADER = "Donor,Item,Category,Quantity,Unit,Expiry Date,Donation Date";

    private long rice;
    private long milk;

    @BeforeEach
    void seedMasterData() {
        long food = createCategory("Food");
        rice = createItem(food, "Rice", 100, 20);
        milk = createItem(food, "Milk", 30, 5);
        createDonor("Jane Doe");
    }

    @Test
    @DisplayName("The template endpoint returns the expected CSV header")
    void templateHeader() throws Exception {
        MvcResult result = mockMvc.perform(authGet("/api/donations/bulk/template", staffToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).startsWith(HEADER);
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
    }

    @Test
    @DisplayName("Preview validates every row and saves nothing")
    void previewSavesNothing() throws Exception {
        String csv = HEADER + "\n"
                + "Jane Doe,Rice,Food,50,KG," + LocalDate.now().plusMonths(6) + "," + LocalDate.now() + "\n"
                + "Jane Doe,Milk,Food,20,KG,," + LocalDate.now() + "\n";

        mockMvc.perform(upload("/api/donations/bulk/preview", csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.validRows").value(2))
                .andExpect(jsonPath("$.data.invalidRows").value(0))
                .andExpect(jsonPath("$.data.donationsToCreate").value(1));

        // Dry run: stock and donation history are untouched.
        assertThat(quantityOf(rice)).isEqualTo(100);
        assertThat(quantityOf(milk)).isEqualTo(30);
        mockMvc.perform(authGet("/api/donations", staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Preview reports every bad row with its row number and reasons")
    void previewReportsInvalidRows() throws Exception {
        String csv = HEADER + "\n"
                + "Jane Doe,Rice,Food,50,KG,," + LocalDate.now() + "\n"
                + ",Rice,Food,10,KG,," + LocalDate.now() + "\n"
                + "Jane Doe,Unobtainium,Food,10,KG,," + LocalDate.now() + "\n"
                + "Jane Doe,Rice,Food,-5,KG,," + LocalDate.now() + "\n"
                + "Jane Doe,Rice,Food,10,KG,not-a-date," + LocalDate.now() + "\n";

        MvcResult result = mockMvc.perform(upload("/api/donations/bulk/preview", csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(5))
                .andExpect(jsonPath("$.data.validRows").value(1))
                .andExpect(jsonPath("$.data.invalidRows").value(4))
                .andReturn();

        JsonNode rows = body(result).path("data").path("rows");
        assertThat(rows).hasSize(5);
        assertThat(rows.get(1).path("rowNumber").asInt()).isEqualTo(2);
        assertThat(rows.get(1).path("errors").toString()).contains("Donor");
        assertThat(rows.get(2).path("errors").toString()).contains("Unobtainium");
        assertThat(rows.get(3).path("errors").toString()).contains("greater than zero");
        assertThat(rows.get(4).path("errors").toString()).contains("Expiry date");
    }

    @Test
    @DisplayName("A valid import creates donations, raises stock and writes the ledger")
    void importCreatesDonations() throws Exception {
        String csv = HEADER + "\n"
                + "Jane Doe,Rice,Food,50,KG,," + LocalDate.now() + "\n"
                + "Jane Doe,Milk,Food,20,KG,," + LocalDate.now() + "\n";

        mockMvc.perform(upload("/api/donations/bulk/import", csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.successfulRows").value(2))
                .andExpect(jsonPath("$.data.failedRows").value(0))
                .andExpect(jsonPath("$.data.createdDonations").value(1))
                .andExpect(jsonPath("$.data.totalQuantityImported").value(70));

        assertThat(quantityOf(rice)).isEqualTo(150);
        assertThat(quantityOf(milk)).isEqualTo(50);

        mockMvc.perform(authGet("/api/inventory-transactions?transactionType=DONATION_IN", staffToken))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("By default one bad row aborts the entire import — nothing is written")
    void importIsAllOrNothing() throws Exception {
        String csv = HEADER + "\n"
                + "Jane Doe,Rice,Food,50,KG,," + LocalDate.now() + "\n"
                + "Jane Doe,Unobtainium,Food,10,KG,," + LocalDate.now() + "\n";

        // The abort is reported as a summary rather than an error: the upload itself was well-formed,
        // it simply imported nothing.
        mockMvc.perform(upload("/api/donations/bulk/import", csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successfulRows").value(0))
                .andExpect(jsonPath("$.data.createdDonations").value(0))
                .andExpect(jsonPath("$.data.failedRows").value(1))
                .andExpect(jsonPath("$.data.errors.length()").value(1))
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("aborted")));

        assertThat(quantityOf(rice)).isEqualTo(100);
        mockMvc.perform(authGet("/api/donations", staffToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("allowPartial imports the good rows and returns the rejected ones")
    void partialImportReportsRejectedRows() throws Exception {
        String csv = HEADER + "\n"
                + "Jane Doe,Rice,Food,50,KG,," + LocalDate.now() + "\n"
                + "Jane Doe,Unobtainium,Food,10,KG,," + LocalDate.now() + "\n";

        MvcResult result = mockMvc.perform(upload("/api/donations/bulk/import?allowPartial=true", csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successfulRows").value(1))
                .andExpect(jsonPath("$.data.failedRows").value(1))
                .andReturn();

        // Rejected rows are reported, never silently dropped.
        assertThat(body(result).path("data").path("errors")).hasSize(1);
        assertThat(quantityOf(rice)).isEqualTo(150);
    }

    @Test
    @DisplayName("A file with the wrong header is rejected before any parsing")
    void wrongHeaderRejected() throws Exception {
        mockMvc.perform(upload("/api/donations/bulk/import", "Name,Thing\nJane,Rice\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_COLUMN"));
    }

    @Test
    @DisplayName("Repeating an import under the same Idempotency-Key does not double the stock")
    void idempotentImport() throws Exception {
        String csv = HEADER + "\n" + "Jane Doe,Rice,Food,50,KG,," + LocalDate.now() + "\n";

        mockMvc.perform(upload("/api/donations/bulk/import", csv)
                        .header("Idempotency-Key", "bulk-key-1"))
                .andExpect(status().isOk());
        assertThat(quantityOf(rice)).isEqualTo(150);

        mockMvc.perform(upload("/api/donations/bulk/import", csv)
                        .header("Idempotency-Key", "bulk-key-1"))
                .andExpect(status().isOk());

        // The retry is recognised: stock moved once, not twice.
        assertThat(quantityOf(rice)).isEqualTo(150);
        mockMvc.perform(authGet("/api/donations", staffToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    private MockHttpServletRequestBuilder upload(String url, String csv) {
        MockMultipartFile file = new MockMultipartFile("file", "donations.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        return multipart(url).file(file).header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken);
    }
}
