package com.jurisfacil.shared.tenant;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public final class TenantFilter implements Filter {

    private static final String ORGANIZATION_HEADER = "X-Organization-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                UUID organizationId = parseOrganizationId(httpRequest.getHeader(ORGANIZATION_HEADER));
                if (organizationId != null) {
                    TenantContextHolder.set(new TenantContext(organizationId, null, List.of()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID parseOrganizationId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
