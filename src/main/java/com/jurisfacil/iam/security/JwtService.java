package com.jurisfacil.iam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "juris-facil";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String issueAccessToken(JwtClaims claims) {
        return Jwts.builder()
                .subject(claims.sub().toString())
                .issuer(ISSUER)
                .issuedAt(java.util.Date.from(claims.iat()))
                .expiration(java.util.Date.from(claims.exp()))
                .id(claims.jti())
                .claim("name", claims.name())
                .claim("email", claims.email())
                .claim("organization_id", claims.organizationId() == null ? null : claims.organizationId().toString())
                .claim("role", claims.role())
                .claim("platform_roles", claims.platformRoles())
                .claim("entitlements", claims.entitlements())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public JwtClaims parseAndVerify(String token) {
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token);
        Claims payload = parsed.getPayload();
        return new JwtClaims(
                UUID.fromString(payload.getSubject()),
                payload.get("name", String.class),
                payload.get("email", String.class),
                readUuid(payload, "organization_id"),
                payload.get("role", String.class),
                readStringList(payload, "platform_roles"),
                readStringList(payload, "entitlements"),
                payload.getIssuedAt().toInstant(),
                payload.getExpiration().toInstant(),
                payload.getId());
    }

    private static UUID readUuid(Claims claims, String name) {
        String value = claims.get(name, String.class);
        return value == null ? null : UUID.fromString(value);
    }

    private static List<String> readStringList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("JWT claim must be a list: " + name);
        }
        return values.stream().map(String.class::cast).toList();
    }
}
