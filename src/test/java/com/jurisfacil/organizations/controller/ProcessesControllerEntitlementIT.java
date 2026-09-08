package com.jurisfacil.organizations.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class ProcessesControllerEntitlementIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JwtService jwtService;

    @BeforeEach
    void cleanOrganizations() {
        jdbcTemplate.update("DELETE FROM memberships");
        jdbcTemplate.update("DELETE FROM module_entitlements");
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM organizations");
    }

    @Test
    void rejectsDisabledProcessModuleWithStableError() throws Exception {
        UUID organizationId = createOrganization();
        insertEntitlement(organizationId, false);

        mockMvc.perform(get("/api/v1/processes")
                .header("Authorization", "Bearer " + token(organizationId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MODULE_DISABLED"));
    }

    @Test
    void returnsEmptyPagedCollectionWhenProcessModuleIsEnabled() throws Exception {
        UUID organizationId = createOrganization();
        insertEntitlement(organizationId, true);

        mockMvc.perform(get("/api/v1/processes")
                .header("Authorization", "Bearer " + token(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void returnsZeroSummaryWhenProcessModuleIsEnabled() throws Exception {
        UUID organizationId = createOrganization();
        insertEntitlement(organizationId, true);

        mockMvc.perform(get("/api/v1/processes/summary")
                .header("Authorization", "Bearer " + token(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProcesses").value(0))
                .andExpect(jsonPath("$.urgentDeadlines").value(0))
                .andExpect(jsonPath("$.upcomingHearings").value(0))
                .andExpect(jsonPath("$.documentsCount").value(0));
    }

    private UUID createOrganization() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email, status) VALUES (?, ?, 'ACTIVE') RETURNING organization_id",
                UUID.class, "Process Guard Test", "guard-" + UUID.randomUUID() + "@example.com");
    }

    private void insertEntitlement(UUID organizationId, boolean enabled) {
        jdbcTemplate.update(
                "INSERT INTO module_entitlements (organization_id, module_code, enabled) VALUES (?, 'PROCESS', ?)",
                organizationId, enabled);
    }

    private String token(UUID organizationId) {
        Instant now = Instant.now();
        return jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Workspace User",
                "guard@example.com", organizationId, "LAWYER", List.of(), List.of(), now,
                now.plusSeconds(900), UUID.randomUUID().toString()));
    }
}
