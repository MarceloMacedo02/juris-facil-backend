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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class WorkspaceOrganizationControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JwtService jwtService;

    @BeforeEach
    void cleanOrganizations() {
        jdbcTemplate.update("DELETE FROM memberships");
        jdbcTemplate.update("DELETE FROM organizations");
    }

    @Test
    void returnsOrganizationFromWorkspaceToken() throws Exception {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO organizations "
                + "(organization_id, name, cnpj_cpf, contact_email, phone, city, state, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')",
                organizationId, "Juris-Fácil", "12.345.678/0001-90", "contato@example.com",
                "+55 85 99999-0000", "Fortaleza", "CE");

        mockMvc.perform(get("/api/v1/organization")
                .header("Authorization", "Bearer " + token(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId.toString()))
                .andExpect(jsonPath("$.name").value("Juris-Fácil"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsTokenWithoutOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/organization")
                .header("Authorization", "Bearer " + token(null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void hidesMissingOrganizationAsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/organization")
                .header("Authorization", "Bearer " + token(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private String token(UUID organizationId) {
        Instant now = Instant.now();
        return jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Workspace User",
                "workspace@example.com", organizationId, "LAWYER", List.of(), List.of(), now,
                now.plusSeconds(900), UUID.randomUUID().toString()));
    }
}
