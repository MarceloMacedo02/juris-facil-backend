package com.jurisfacil.shared.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CorrelationIdIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void echoesCorrelationIdThroughTheHttpPipeline() throws Exception {
        mockMvc.perform(post("/api/v1/_dev/echo")
                        .header(CorrelationIdFilter.HEADER, "abc123")
                        .header("Authorization", "Bearer " + token())
                        .contentType("application/json")
                        .content("{\"name\":\"Juris-Fácil\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc123"));
    }

    private String token() {
        Instant now = Instant.now();
        return jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Test", "test@example.com",
                UUID.randomUUID(), "LAWYER", List.of(), List.of(), now, now.plusSeconds(900), UUID.randomUUID().toString()));
    }
}
