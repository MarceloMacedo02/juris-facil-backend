package com.jurisfacil.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantFilterIT {

    private final TenantFilter tenantFilter = new TenantFilter();

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
    }

    @Test
    void populatesContextDuringRequestAndClearsItAfterward() throws Exception {
        UUID organizationId = UUID.randomUUID();
        MockHttpServletRequest request = requestWithOrganization(organizationId);
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
        MockHttpServletRequest request = requestWithOrganization(UUID.randomUUID());
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
    void ignoresMissingAndMalformedOptionalHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.get()).isNull());

        request.addHeader("X-Organization-Id", "not-a-uuid");
        tenantFilter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.get()).isNull());

        assertThat(TenantContextHolder.get()).isNull();
    }

    private MockHttpServletRequest requestWithOrganization(UUID organizationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Organization-Id", organizationId.toString());
        return request;
    }
}
