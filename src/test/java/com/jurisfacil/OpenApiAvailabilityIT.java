package com.jurisfacil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
class OpenApiAvailabilityIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocumentWithJurisFacilMetadataAndHealthTag() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Juris-Fácil API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0-P0"))
                .andExpect(jsonPath("$.info.description").value("API do MVP P0"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.paths['/api/v1/_dev/echo'].post.tags", Matchers.hasItem("Health Dev")));
    }

    @Test
    void exposesGroupedWorkspaceDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/_dev/echo']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.paths['/api/v1/_dev/echo'].post.security[0].bearerAuth").isEmpty());
    }

    @Test
    void exposesGroupedAdminDocumentAndDoesNotLeakWorkspacePaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/_dev/echo']").doesNotExist())
                .andExpect(jsonPath("$.paths").isEmpty());
    }
}
