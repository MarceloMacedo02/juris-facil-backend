package com.jurisfacil.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void rejectsSixthRequestFromSameIpWithinTheConfiguredWindow() throws Exception {
        RateLimitConfig config = new RateLimitConfig();
        config.setPaths(java.util.List.of("/api/v1/_dev/echo"));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(config, objectMapper);

        for (int attempt = 1; attempt <= 5; attempt++) {
            MockHttpServletResponse response = invoke(filter, "203.0.113.10");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse limitedResponse = invoke(filter, "203.0.113.10");

        assertThat(limitedResponse.getStatus()).isEqualTo(429);
        assertThat(limitedResponse.getHeader("Retry-After")).isNotBlank();
        assertThat(limitedResponse.getContentAsString())
                .contains("\"code\":\"RATE_LIMIT_EXCEEDED\"")
                .contains("\"status\":429");
    }

    @Test
    void doesNotLimitPathsOutsideConfiguredList() throws Exception {
        RateLimitConfig config = new RateLimitConfig();
        config.setPaths(java.util.List.of("/api/v1/_dev/echo"));
        RateLimitFilter filter = new RateLimitFilter(config, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(204));

        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void bypassesGetOnConfiguredPathWithoutConsumingPostBucket() throws Exception {
        RateLimitConfig config = new RateLimitConfig();
        config.setPaths(java.util.List.of("/api/v1/_dev/echo"));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(config, objectMapper);

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(invoke(filter, "POST", "203.0.113.11").getStatus()).isEqualTo(200);
        }

        assertThat(invoke(filter, "GET", "203.0.113.11").getStatus()).isEqualTo(200);
        assertThat(invoke(filter, "POST", "203.0.113.11").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse invoke(RateLimitFilter filter, String ipAddress) throws Exception {
        return invoke(filter, "POST", ipAddress);
    }

    private MockHttpServletResponse invoke(RateLimitFilter filter, String method, String ipAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/_dev/echo");
        request.setRemoteAddr(ipAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(200));
        return response;
    }
}
