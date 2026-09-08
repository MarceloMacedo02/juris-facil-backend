package com.jurisfacil.organizations.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class OrganizationStatusIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
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
    void suspendsAndReactivatesOrganizationWithSubscription() throws Exception {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO organizations (organization_id, name, cnpj_cpf, contact_email, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                organizationId, "Status Office", "04.252.011/0001-10", "status@example.com");
        jdbcTemplate.update("INSERT INTO subscriptions (subscription_id, organization_id, plan_tier, status) VALUES (?, ?, 'PROFISSIONAL', 'ACTIVE')",
                UUID.randomUUID(), organizationId);

        mockMvc.perform(put("/api/admin/v1/organizations/{id}/status", organizationId)
                .header("Authorization", "Bearer " + adminToken())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new StatusPayload("SUSPENDED", "billing"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.currentStatus").value("SUSPENDED"));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM subscriptions WHERE organization_id = ?", String.class, organizationId))
                .isEqualTo("SUSPENDED");

        mockMvc.perform(put("/api/admin/v1/organizations/{id}/status", organizationId)
                .header("Authorization", "Bearer " + adminToken())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new StatusPayload("ACTIVE", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("ACTIVE"));
    }

    @Test
    void supportCannotChangeOrganizationStatus() throws Exception {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO organizations (organization_id, name, cnpj_cpf, contact_email, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                organizationId, "Support Office", "04.252.011/0001-10", "support@example.com");

        mockMvc.perform(put("/api/admin/v1/organizations/{id}/status", organizationId)
                .header("Authorization", "Bearer " + adminToken("SUPPORT"))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new StatusPayload("SUSPENDED", null))))
                .andExpect(status().isForbidden());
    }

    private String adminToken() {
        return adminToken("PLATFORM_ADMIN");
    }

    private String adminToken(String role) {
        Instant now = Instant.now();
        return jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Admin", "admin@example.com", null,
                null, List.of(role), List.of(), now, now.plusSeconds(900), UUID.randomUUID().toString()));
    }

    private record StatusPayload(String status, String reason) {
    }
}
