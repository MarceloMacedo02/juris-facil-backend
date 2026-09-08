package com.jurisfacil.organizations.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminOnboardingIT extends BaseIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'onboarding-%'");
    }

    @Test
    void createsOrganizationAndOwnerInAtomicFlow() throws Exception {
        mockMvc.perform(post("/api/admin/v1/organizations")
                .header("Authorization", "Bearer " + adminToken())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request("onboarding-success@example.com", "04.252.011/0001-10"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").isString())
                .andExpect(jsonPath("$.ownerMembershipId").isString())
                .andExpect(jsonPath("$.activationToken").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(count("organizations")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(count("subscriptions")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(count("module_entitlements")).isEqualTo(8);
        org.assertj.core.api.Assertions.assertThat(countEnabledEntitlements()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(count("memberships")).isEqualTo(1);
    }

    @Test
    void rollsBackWhenOwnerEmailIsAlreadyUsed() throws Exception {
        jdbcTemplate.update("INSERT INTO users (user_id, name, email, status) VALUES (?, ?, ?, 'ACTIVE')",
                UUID.randomUUID(), "Existing", "onboarding-existing@example.com");

        mockMvc.perform(post("/api/admin/v1/organizations")
                .header("Authorization", "Bearer " + adminToken())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request("onboarding-existing@example.com", "04.252.011/0001-10"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));

        org.assertj.core.api.Assertions.assertThat(count("organizations")).isZero();
    }

    @Test
    void rejectsInvalidDocumentBeforeWriting() throws Exception {
        mockMvc.perform(post("/api/admin/v1/organizations")
                .header("Authorization", "Bearer " + adminToken())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request("onboarding-invalid@example.com", "04.252.011/0001-11"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        org.assertj.core.api.Assertions.assertThat(count("organizations")).isZero();
    }

    private CreateOrganizationPayload request(String ownerEmail, String document) {
        return new CreateOrganizationPayload("Onboarding Office", document, "contact@example.com",
                "Owner Name", ownerEmail, "PROFISSIONAL", null, "Fortaleza", "CE");
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long countEnabledEntitlements() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM module_entitlements WHERE enabled = true", Long.class);
    }

    private String adminToken() {
        Instant now = Instant.now();
        return jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Platform Admin",
                "admin@example.com", null, null, List.of("PLATFORM_ADMIN"), List.of(), now,
                now.plusSeconds(900), UUID.randomUUID().toString()));
    }

    private record CreateOrganizationPayload(String name, String cnpjCpf, String contactEmail,
            String ownerName, String ownerEmail, String planTier, String phone, String city, String state) {
    }
}
