package com.jurisfacil.iam.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JwtClaims(
        UUID sub,
        String name,
        String email,
        UUID organizationId,
        String role,
        List<String> platformRoles,
        List<String> entitlements,
        Instant iat,
        Instant exp,
        String jti) {

    public JwtClaims {
        platformRoles = platformRoles == null ? List.of() : List.copyOf(platformRoles);
        entitlements = entitlements == null ? List.of() : List.copyOf(entitlements);
    }
}
