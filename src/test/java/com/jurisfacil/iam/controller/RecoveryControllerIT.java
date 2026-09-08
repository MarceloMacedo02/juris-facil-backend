package com.jurisfacil.iam.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.jurisfacil.support.BaseIntegrationTest;
import com.jurisfacil.iam.service.RecoveryService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class RecoveryControllerIT extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM refresh_sessions");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void forgotPasswordAlwaysReturnsAcceptedAndStoresHashedToken() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id, email, name, password_hash, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                userId, "person@example.com", "Person", new BCryptPasswordEncoder().encode("password"));

        mockMvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                        .content("{\"email\":\"person@example.com\"}"))
                .andExpect(status().isAccepted());

        MapRow row = jdbcTemplate.queryForObject("SELECT reset_token_hash, reset_expires_at FROM users WHERE user_id = ?",
                (rs, n) -> new MapRow(rs.getString(1), rs.getObject(2, OffsetDateTime.class)), userId);
        org.assertj.core.api.Assertions.assertThat(row.hash()).hasSize(64);
        org.assertj.core.api.Assertions.assertThat(row.expiresAt()).isAfter(OffsetDateTime.now().plusMinutes(29));
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void resetPasswordReturnsNoContentAndInvalidatesRefreshCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        String rawToken = "integration-reset-token";
        String hash = RecoveryService.sha256(rawToken);
        jdbcTemplate.update("INSERT INTO users (user_id, email, name, password_hash, status, reset_token_hash, reset_expires_at) VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)",
                userId, "person@example.com", "Person", new BCryptPasswordEncoder().encode("password"), hash,
                OffsetDateTime.now().plusMinutes(30));
        UUID sessionId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO refresh_sessions (session_id, user_id, token_hash, expires_at, platform_session, remember_me) VALUES (?, ?, ?, ?, false, false)",
                sessionId, userId, "session-hash", OffsetDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/auth/reset-password").contentType("application/json")
                        .content("{\"token\":\"" + rawToken + "\",\"new_password\":\"new-password\"}"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JF_REFRESH", 0));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE user_id = ?", String.class, userId))
                .startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT reset_token_hash FROM users WHERE user_id = ?", String.class, userId)).isNull();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM refresh_sessions WHERE session_id = ?", OffsetDateTime.class, sessionId)).isNotNull();
    }

    @Test
    void resetPasswordReturnsStableErrorForInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType("application/json")
                        .content("{\"token\":\"invalid\",\"new_password\":\"new-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    private record MapRow(String hash, OffsetDateTime expiresAt) { }
}
