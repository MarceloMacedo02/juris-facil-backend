package com.jurisfacil.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RateLimitEndToEndIT {

    private static final String TEST_IP = "198.51.100.77";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void returnsRateLimitProblemDetailsOnSixthEchoRequest() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(echoRequest())
                    .andExpect(status().isOk());
        }

        mockMvc.perform(echoRequest())
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder echoRequest() {
        return post("/api/v1/_dev/echo")
                .with(remoteAddress(TEST_IP))
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Juris-Fácil\"}");
    }

    private String token() {
        Instant now = Instant.now();
        return jwtService.issueAccessToken(new JwtClaims(UUID.randomUUID(), "Test", "test@example.com",
                UUID.randomUUID(), "LAWYER", List.of(), List.of(), now, now.plusSeconds(900), UUID.randomUUID().toString()));
    }

    private RequestPostProcessor remoteAddress(String ipAddress) {
        return request -> {
            request.setRemoteAddr(ipAddress);
            return request;
        };
    }
}
