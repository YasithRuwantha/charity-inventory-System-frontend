package com.charitymanagement.api.charitymanagementback.support;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import com.charitymanagement.api.charitymanagementback.auth.repository.UserRepository;
import com.charitymanagement.api.charitymanagementback.auth.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared plumbing for the HTTP-level tests: a booted application on an in-memory schema, MockMvc,
 * and real JWTs minted through the production {@code JwtService} so the existing authentication
 * filter is genuinely exercised rather than stubbed out.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@charity.test";
    protected static final String STAFF_EMAIL = "staff@charity.test";
    protected static final String VOLUNTEER_EMAIL = "volunteer@charity.test";
    protected static final String PASSWORD = "Password@123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected DatabaseCleaner databaseCleaner;

    protected String adminToken;
    protected String staffToken;
    protected String volunteerToken;

    protected Long adminId;
    protected Long staffId;

    /**
     * Every test starts from an empty schema, so counts, dashboard figures and generated references
     * are deterministic regardless of execution order.
     */
    @BeforeEach
    void resetAndSeedAccounts() {
        databaseCleaner.clean();
        User admin = ensureUser(ADMIN_EMAIL, "Admin User", UserRole.ADMIN);
        User staff = ensureUser(STAFF_EMAIL, "Inventory Staff", UserRole.INVENTORY_STAFF);
        adminId = admin.getId();
        staffId = staff.getId();
        adminToken = tokenFor(admin);
        staffToken = tokenFor(staff);
        volunteerToken = tokenFor(ensureUser(VOLUNTEER_EMAIL, "Volunteer User", UserRole.VOLUNTEER));
    }

    protected User ensureUser(String email, String name, UserRole role) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build()));
    }

    protected String tokenFor(User user) {
        return jwtService.generateToken(user);
    }

    // ── Request helpers ─────────────────────────────────────────────────────

    protected MockHttpServletRequestBuilder authGet(String url, String token) {
        return get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder authPost(String url, String token, Object body) {
        MockHttpServletRequestBuilder builder = post(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return withBody(builder, body);
    }

    protected MockHttpServletRequestBuilder authPut(String url, String token, Object body) {
        return withBody(put(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token), body);
    }

    protected MockHttpServletRequestBuilder authPatch(String url, String token, Object body) {
        return withBody(patch(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token), body);
    }

    protected MockHttpServletRequestBuilder authDelete(String url, String token) {
        return delete(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder, Object body) {
        if (body == null) {
            return builder;
        }
        return builder.contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected String json(Object body) {
        try {
            return body instanceof String s ? s : objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialise test payload", ex);
        }
    }

    /** The parsed response body, so assertions can navigate {@code data}, {@code errorCode} etc. */
    protected JsonNode body(MvcResult result) {
        try {
            return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse response body", ex);
        }
    }

    protected long id(MvcResult result) {
        return body(result).path("data").path("id").asLong();
    }

    // ── Fixtures ────────────────────────────────────────────────────────────
    // Built over the real HTTP API rather than the repositories, so the fixtures themselves prove
    // the create endpoints work and every row goes through the same validation as production.

    protected long createCategory(String name) {
        MvcResult result = perform(authPost("/api/categories", staffToken,
                Map.of("name", name, "description", name + " donations")), 201);
        return id(result);
    }

    protected long createItem(long categoryId, String itemName, int quantity, int minimumStockLevel) {
        return createItem(categoryId, itemName, quantity, minimumStockLevel, null);
    }

    protected long createItem(long categoryId, String itemName, int quantity, int minimumStockLevel,
                              LocalDate expiryDate) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("itemName", itemName);
        payload.put("categoryId", categoryId);
        payload.put("quantity", quantity);
        payload.put("unit", "KG");
        payload.put("minimumStockLevel", minimumStockLevel);
        if (expiryDate != null) {
            payload.put("expiryDate", expiryDate.toString());
        }
        return id(perform(authPost("/api/inventory", staffToken, payload), 201));
    }

    protected long createDonor(String donorName) {
        return id(perform(authPost("/api/donors", staffToken,
                Map.of("donorName", donorName, "donorType", "INDIVIDUAL",
                        "email", donorName.toLowerCase(Locale.ROOT).replace(" ", ".") + "@donor.test")), 201));
    }

    protected long createBeneficiary(String name, int familySize, String priority) {
        return id(perform(authPost("/api/beneficiaries", staffToken,
                Map.of("beneficiaryName", name, "familySize", familySize, "priorityLevel", priority)), 201));
    }

    /** Volunteer registration is ADMIN-only, so this fixture deliberately uses the admin token. */
    protected long createVolunteer(String name) {
        return id(perform(authPost("/api/volunteers", adminToken,
                Map.of("volunteerName", name, "joinedDate", LocalDate.now().toString())), 201));
    }

    /** Reads the live quantity straight from the inventory API. */
    protected int quantityOf(long itemId) {
        return body(perform(authGet("/api/inventory/" + itemId, staffToken), 200))
                .path("data").path("quantity").asInt();
    }

    protected String statusOf(long itemId) {
        return body(perform(authGet("/api/inventory/" + itemId, staffToken), 200))
                .path("data").path("status").asText();
    }

    /** Executes a request and asserts the status code, returning the result for further assertions. */
    protected MvcResult perform(MockHttpServletRequestBuilder request, int expectedStatus) {
        try {
            return mockMvc.perform(request)
                    .andExpect(status().is(expectedStatus))
                    .andReturn();
        } catch (Exception ex) {
            throw new IllegalStateException("Request failed", ex);
        }
    }
}
