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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.support.BaseIntegrationTest;

@AutoConfigureMockMvc
class AdminAuthControllerIT extends BaseIntegrationTest {
        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private JdbcTemplate jdbcTemplate;
        @Autowired
        private JwtService jwtService;
        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void cleanDatabase() {
                jdbcTemplate.update("DELETE FROM refresh_sessions");
                jdbcTemplate.update("DELETE FROM users");
        }

        @Test
        void adminLoginReturnsSeparateCookieAndPlatformClaims() throws Exception {
                UUID userId = UUID.randomUUID();
                jdbcTemplate.update(
                                "INSERT INTO users (user_id, email, name, password_hash, platform_role, status) VALUES (?, ?, ?, ?, 'PLATFORM_ADMIN', 'ACTIVE')",
                                userId, "admin@example.com", "Platform Admin",
                                new BCryptPasswordEncoder().encode("password"));

                MvcResult result = mockMvc.perform(post("/api/admin/v1/auth/login").contentType("application/json")
                                .content("{\"email\":\"admin@example.com\",\"password\":\"password\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.access_token").isString())
                                .andExpect(jsonPath("$.roles[0]").value("PLATFORM_ADMIN"))
                                .andExpect(cookie().path("JF_ADMIN_REFRESH", "/api/admin"))
                                .andExpect(cookie().httpOnly("JF_ADMIN_REFRESH", true))
                                .andExpect(cookie().secure("JF_ADMIN_REFRESH", true))
                                .andReturn();

                String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                                .get("access_token").asText();
                var claims = jwtService.parseAndVerify(accessToken);
                org.assertj.core.api.Assertions.assertThat(claims.organizationId()).isNull();
                org.assertj.core.api.Assertions.assertThat(claims.platformRoles()).containsExactly("PLATFORM_ADMIN");

                Integer platformSessions = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM refresh_sessions WHERE user_id = ? AND platform_session = true AND organization_id IS NULL",
                                Integer.class, userId);
                org.assertj.core.api.Assertions.assertThat(platformSessions).isEqualTo(1);
        }

        @Test
        void workspaceUserReceivesGenericUnauthorized() throws Exception {
                jdbcTemplate.update("INSERT INTO users (email, name, password_hash, status) VALUES (?, ?, ?, 'ACTIVE')",
                                "workspace@example.com", "Workspace", new BCryptPasswordEncoder().encode("password"));

                mockMvc.perform(post("/api/admin/v1/auth/login").contentType("application/json")
                                .content("{\"email\":\"workspace@example.com\",\"password\":\"password\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
}
