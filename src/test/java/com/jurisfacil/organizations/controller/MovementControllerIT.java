package com.jurisfacil.organizations.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MovementControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/processes/{id}/movements", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsPagedTimelineForAuthenticatedTenant() throws Exception {
        UUID organizationId = insertOrganization();
        UUID processId = insertProcess(organizationId);
        UUID userId = insertUser();
        jdbcTemplate.update(
                "INSERT INTO memberships (organization_id, user_id, role, status) VALUES (?, ?, 'LAWYER', 'ACTIVE')",
                organizationId, userId);
        jdbcTemplate.update(
                "INSERT INTO process_movements (organization_id, process_id, movement_date, movement_type, title) "
                        + "VALUES (?, ?, '2026-02-02T10:00:00Z', 'OUTRO', 'Newest')",
                organizationId, processId);
        String token = jwtService.issueAccessToken(new JwtClaims(userId, "Timeline User",
                "controller@example.com", organizationId, "LAWYER", List.of(), List.of("PROCESS"),
                Instant.now(), Instant.now().plusSeconds(900), UUID.randomUUID().toString()));

        mockMvc.perform(get("/api/v1/processes/{id}/movements", processId)
                        .param("size", "50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Newest"))
                .andExpect(jsonPath("$.items[0].movementType").value("OUTRO"))
                .andExpect(jsonPath("$.size").value(50));
    }

    private UUID insertOrganization() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email) VALUES (?, ?) RETURNING organization_id",
                UUID.class, "Movement Controller Office", "controller-" + UUID.randomUUID() + "@example.com");
    }

    private UUID insertProcess(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO processes (organization_id, title, court, court_unit, location) "
                        + "VALUES (?, 'Controller Process', 'TJCE', 'Unit', 'Fortaleza') RETURNING process_id",
                UUID.class, organizationId);
    }

    private UUID insertUser() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (name, email, status) VALUES ('Timeline User', ?, 'ACTIVE') RETURNING user_id",
                UUID.class, "controller-user-" + UUID.randomUUID() + "@example.com");
    }
}
