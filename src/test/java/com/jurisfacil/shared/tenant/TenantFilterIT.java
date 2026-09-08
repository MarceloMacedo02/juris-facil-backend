package com.jurisfacil.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.jurisfacil.iam.security.JwtClaims;

class TenantFilterIT {

    private final TenantFilter tenantFilter = new TenantFilter();

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesContextDuringRequestAndClearsItAfterward() throws Exception {
        UUID organizationId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        authenticate(organizationId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            TenantContext context = TenantContextHolder.get();
            assertThat(context).isNotNull();
            assertThat(context.organizationId()).isEqualTo(organizationId);
        };

        tenantFilter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void clearsContextEvenWhenDownstreamChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        authenticate(UUID.randomUUID());
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("downstream failure");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        tenantFilter.doFilter(request, response, (servletRequest, servletResponse) -> {
                            throw failure;
                        }))
                .isSameAs(failure);

        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void ignoresMissingAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.get()).isNull());

        assertThat(TenantContextHolder.get()).isNull();
    }

    private void authenticate(UUID organizationId) {
        Instant now = Instant.now();
        JwtClaims claims = new JwtClaims(UUID.randomUUID(), "User", "user@example.com", organizationId,
                "LAWYER", List.of(), List.of("PROCESS"), now, now.plusSeconds(900), UUID.randomUUID().toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(claims, null, List.of()));
    }
}
