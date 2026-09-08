package com.jurisfacil.iam.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "jurisfacil-unit-test-secret-32-bytes!!";

    private final JwtService service = new JwtService(SECRET);

    @Test
    void issuesAndVerifiesAllAccessClaims() {
        JwtClaims expected = claims(Instant.now(), Instant.now().plusSeconds(900));

        JwtClaims actual = service.parseAndVerify(service.issueAccessToken(expected));

        assertEquals(expected, actual);
    }

    @Test
    void rejectsExpiredToken() {
        JwtClaims expired = claims(Instant.now().minusSeconds(901), Instant.now().minusSeconds(1));

        assertThrows(ExpiredJwtException.class, () -> service.parseAndVerify(service.issueAccessToken(expired)));
    }

    @Test
    void rejectsTokenWithInvalidSignature() {
        JwtClaims expected = claims(Instant.now(), Instant.now().plusSeconds(900));
        String token = Jwts.builder()
                .subject(expected.sub().toString())
                .issuer("juris-facil")
                .issuedAt(java.util.Date.from(expected.iat()))
                .expiration(java.util.Date.from(expected.exp()))
                .id(expected.jti())
                .signWith(Keys.hmacShaKeyFor("another-unit-test-secret-32-bytes!!".getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThrows(SignatureException.class, () -> service.parseAndVerify(token));
    }

    private static JwtClaims claims(Instant issuedAt, Instant expiration) {
        return new JwtClaims(
                UUID.randomUUID(),
                "Maria Silva",
                "maria@example.com",
                UUID.randomUUID(),
                "OWNER",
                List.of(),
                List.of("PROCESS"),
                issuedAt.truncatedTo(ChronoUnit.SECONDS),
                expiration.truncatedTo(ChronoUnit.SECONDS),
                UUID.randomUUID().toString());
    }
}
