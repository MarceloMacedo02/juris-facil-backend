package com.jurisfacil.iam.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jurisfacil.support.BaseIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class RefreshControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM refresh_sessions");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void rotatesCookieAndPersistsReplacement() throws Exception {
        UUID userId = insertUser();
        String raw = rawToken();
        UUID sessionId = insertSession(userId, raw, OffsetDateTime.now().plusDays(1), false);

        var result = mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("JF_REFRESH", raw)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(header().string("Set-Cookie", containsString("JF_REFRESH=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        String nextRaw = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        assertNotSameRaw(nextRaw, raw);
        Integer revoked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_sessions WHERE session_id = ? AND revoked_at IS NOT NULL AND replaced_by IS NOT NULL",
                Integer.class, sessionId);
        Integer created = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_sessions WHERE token_hash = ? AND token_hash <> ?",
                Integer.class, sha256(nextRaw), raw);
        org.assertj.core.api.Assertions.assertThat(revoked).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(created).isEqualTo(1);
    }

    @Test
    void expiredCookieReturnsInvalidCredentials() throws Exception {
        UUID userId = insertUser();
        String raw = rawToken();
        insertSession(userId, raw, OffsetDateTime.now().minusSeconds(1), false);

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("JF_REFRESH", raw)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void reusedCookieReturnsInvalidCredentialsAndRevokesFamily() throws Exception {
        UUID userId = insertUser();
        String raw = rawToken();
        insertSession(userId, raw, OffsetDateTime.now().plusDays(1), false);

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("JF_REFRESH", raw)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("JF_REFRESH", raw)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        Integer active = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_sessions WHERE user_id = ? AND revoked_at IS NULL", Integer.class, userId);
        org.assertj.core.api.Assertions.assertThat(active).isZero();
    }

    private UUID insertUser() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id, email, name, status) VALUES (?, ?, ?, 'ACTIVE')",
                id, id + "@example.com", "Refresh User");
        return id;
    }

    private UUID insertSession(UUID userId, String raw, OffsetDateTime expiresAt, boolean rememberMe) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO refresh_sessions (session_id, user_id, token_hash, expires_at, remember_me) VALUES (?, ?, ?, ?, ?)",
                id, userId, sha256(raw), expiresAt, rememberMe);
        return id;
    }

    private static String rawToken() {
        byte[] value = new byte[32];
        new java.security.SecureRandom().nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertNotSameRaw(String actual, String original) {
        org.assertj.core.api.Assertions.assertThat(actual).isNotBlank().isNotEqualTo(original);
    }
}
