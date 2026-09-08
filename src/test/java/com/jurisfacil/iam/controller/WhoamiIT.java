package com.jurisfacil.iam.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class WhoamiIT extends BaseIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JwtService jwtService;
    private UUID userId;

    @BeforeEach
    void prepareUser() {
        jdbcTemplate.update("DELETE FROM refresh_sessions");
        jdbcTemplate.update("DELETE FROM users");
        userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id, email, name, password_hash, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                userId, "whoami@example.com", "Who Am I", "hash");
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/_dev/whoami"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenReturnsPersistedUserAndClaims() throws Exception {
        Instant now = Instant.now();
        String token = jwtService.issueAccessToken(new JwtClaims(userId, "Claim Name", "claim@example.com",
                UUID.randomUUID(), "LAWYER", List.of(), List.of("PROCESS_READ"), now, now.plusSeconds(900),
                UUID.randomUUID().toString()));

        mockMvc.perform(get("/api/v1/_dev/whoami").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Who Am I"))
                .andExpect(jsonPath("$.email").value("whoami@example.com"))
                .andExpect(jsonPath("$.role").value("LAWYER"))
                .andExpect(jsonPath("$.entitlements[0]").value("PROCESS_READ"))
                .andExpect(jsonPath("$.organizationId").isString());
    }
}
