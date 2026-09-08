package com.jurisfacil.iam.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

    private final JwtService jwtService = Mockito.mock(JwtService.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validWorkspaceTokenPopulatesClaimsAndAuthorities() throws Exception {
        JwtClaims claims = claims(UUID.randomUUID(), UUID.randomUUID(), "OWNER", List.of(), List.of("PROCESS"));
        when(jwtService.parseAndVerify("token")).thenReturn(claims);

        filter(JwtAuthFilter.Surface.WORKSPACE).doFilter(
                request("token"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(claims);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_OWNER");
    }

    @Test
    void invalidTokenClearsContextAndContinues() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("old", null));
        when(jwtService.parseAndVerify("expired")).thenThrow(new IllegalArgumentException("expired"));
        MockFilterChain chain = new MockFilterChain();

        filter(JwtAuthFilter.Surface.WORKSPACE).doFilter(request("expired"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void workspaceRejectsPlatformToken() throws Exception {
        when(jwtService.parseAndVerify("admin")).thenReturn(
                claims(UUID.randomUUID(), null, null, List.of("PLATFORM_ADMIN"), List.of()));

        filter(JwtAuthFilter.Surface.WORKSPACE).doFilter(
                request("admin"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private JwtAuthFilter filter(JwtAuthFilter.Surface surface) {
        return new JwtAuthFilter(jwtService, surface);
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private JwtClaims claims(UUID id, UUID organizationId, String role, List<String> platformRoles,
            List<String> entitlements) {
        Instant now = Instant.now();
        return new JwtClaims(id, "User", "user@example.com", organizationId, role, platformRoles, entitlements,
                now, now.plusSeconds(900), UUID.randomUUID().toString());
    }
}
