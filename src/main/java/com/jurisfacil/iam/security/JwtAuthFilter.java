package com.jurisfacil.iam.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Shared bearer-token filter for the workspace and platform surfaces. */
public class JwtAuthFilter extends OncePerRequestFilter {

    public enum Surface {
        WORKSPACE,
        ADMIN
    }

    private final JwtService jwtService;
    private final Surface surface;

    public JwtAuthFilter(JwtService jwtService, Surface surface) {
        this.jwtService = jwtService;
        this.surface = surface;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                JwtClaims claims = jwtService.parseAndVerify(authorization.substring("Bearer ".length()));
                if (belongsToSurface(claims)) {
                    SecurityContextHolder.getContext().setAuthentication(authentication(claims));
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean belongsToSurface(JwtClaims claims) {
        return surface == Surface.ADMIN
                ? claims.organizationId() == null && !claims.platformRoles().isEmpty()
                : claims.platformRoles().isEmpty();
    }

    private UsernamePasswordAuthenticationToken authentication(JwtClaims claims) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        claims.platformRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        if (claims.role() != null && !claims.role().isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.role()));
        }
        return new UsernamePasswordAuthenticationToken(claims, null, authorities);
    }
}
