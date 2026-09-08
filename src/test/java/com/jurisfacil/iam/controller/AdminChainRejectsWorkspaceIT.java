package com.jurisfacil.iam.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class AdminChainRejectsWorkspaceIT extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void workspaceTokenCannotEnterAdminSurface() throws Exception {
        String token = jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Workspace", "w@example.com",
                UUID.randomUUID(), "OWNER", List.of(), List.of(), Instant.now(), Instant.now().plusSeconds(900),
                UUID.randomUUID().toString()));

        mockMvc.perform(get("/api/admin/v1/organizations").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedAdminSurfaceIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/v1/organizations"))
                .andExpect(status().isUnauthorized());
    }
}
