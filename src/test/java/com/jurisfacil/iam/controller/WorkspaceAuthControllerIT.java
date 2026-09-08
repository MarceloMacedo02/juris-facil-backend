package com.jurisfacil.iam.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class WorkspaceAuthControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM audit_events");
        jdbcTemplate.update("DELETE FROM refresh_sessions");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void loginReturnsCanonicalSnakeCaseContractAndRefreshCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, name, password_hash, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                userId, "lawyer@example.com", "Lawyer", new BCryptPasswordEncoder().encode("password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"lawyer@example.com\",\"password\":\"password\",\"remember_me\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.user.user_id").value(userId.toString()))
                .andExpect(jsonPath("$.user.email").value("lawyer@example.com"))
                .andExpect(cookie().exists("JF_REFRESH"))
                .andExpect(cookie().value("JF_REFRESH",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(cookie().httpOnly("JF_REFRESH", true))
                .andExpect(cookie().secure("JF_REFRESH", true))
                .andExpect(cookie().path("JF_REFRESH", "/api"))
                .andExpect(cookie().maxAge("JF_REFRESH", 30 * 24 * 60 * 60));
    }

    @Test
    void wrongCredentialsAreGenericUnauthorized() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, name, password_hash, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                UUID.randomUUID(), "lawyer@example.com", "Lawyer", new BCryptPasswordEncoder().encode("password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"lawyer@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logoutRevokesRefreshSessionAndClearsCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, name, password_hash, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                userId, "logout@example.com", "Logout", new BCryptPasswordEncoder().encode("password"));

        var login = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"logout@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = login.getResponse().getHeader("Set-Cookie");
        String rawToken = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));

        mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("JF_REFRESH", rawToken)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JF_REFRESH", 0));

        Integer revoked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_sessions WHERE user_id = ? AND revoked_at IS NOT NULL",
                Integer.class, userId);
        org.assertj.core.api.Assertions.assertThat(revoked).isEqualTo(1);
    }
}
