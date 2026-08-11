package com.libris.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base for tests that exercise the real stack against a real PostgreSQL.
 *
 * <p>The container is a singleton started once for the whole suite rather than per class:
 * spinning up PostgreSQL for every test class would dominate the run time. Testcontainers
 * removes it when the JVM exits.
 *
 * <p>PostgreSQL specifically, not H2: the schema uses partial unique indexes and
 * {@code to_char} aggregation, so testing against a different engine would prove nothing
 * about the migrations that actually ship.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("libris")
            .withUsername("libris")
            .withPassword("libris")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long lastSeededBookId;
    private static Long lastSeededUserId;

    /**
     * Puts the database back to exactly what the migrations left behind.
     *
     * <p>The container is shared by the whole suite for speed, so without this the tests
     * would depend on the order they happen to run in. Rolling the tests back in a
     * transaction is not an option here: the notification flow hangs off
     * {@code AFTER_COMMIT}, and a transaction that never commits would never fire it.
     */
    @BeforeEach
    void resetToSeededState() {
        if (lastSeededBookId == null) {
            lastSeededBookId = jdbcTemplate.queryForObject("select coalesce(max(id), 0) from book", Long.class);
            lastSeededUserId = jdbcTemplate.queryForObject("select coalesce(max(id), 0) from app_user", Long.class);
        }
        jdbcTemplate.update("delete from reservation");
        jdbcTemplate.update("delete from loan");
        jdbcTemplate.update("delete from book_subject where book_id > ?", lastSeededBookId);
        jdbcTemplate.update("delete from book where id > ?", lastSeededBookId);
        jdbcTemplate.update("delete from app_user where id > ?", lastSeededUserId);
        jdbcTemplate.update("update book set status = 'DISPONIBLE'");
        jdbcTemplate.update("update app_user set blocked_until = null");
    }

    /** Signs in and returns the bearer token, the same way a client would. */
    protected String tokenFor(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    protected String adminToken() throws Exception {
        return tokenFor("admin@libris.cl", "Admin123!");
    }

    protected String librarianToken() throws Exception {
        return tokenFor("bibliotecario@libris.cl", "Biblio123!");
    }

    protected String readerToken() throws Exception {
        return tokenFor("lector@libris.cl", "Demo123!");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
