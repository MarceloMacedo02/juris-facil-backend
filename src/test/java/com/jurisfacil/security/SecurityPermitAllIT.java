package com.jurisfacil.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityPermitAllIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedWorkspaceEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/_dev/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juris-Fácil\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminNamespaceIsHandledByOpenPermitAllChain() throws Exception {
        mockMvc.perform(options("/api/admin/v1/_dev/echo")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void workspaceCorsPreflightAllowsConfiguredFrontend() throws Exception {
        mockMvc.perform(options("/api/v1/_dev/echo")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
