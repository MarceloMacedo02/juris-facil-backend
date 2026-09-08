package com.jurisfacil.iam.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.iam.model.entity.RefreshSessionEntity;
import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.RefreshSessionRepository;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.shared.exception.AbstractBusinessException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private static final int TOKEN_BYTES = 32;
    private static final long ACCESS_TOKEN_SECONDS = 900;
    private static final long REMEMBER_ME_SECONDS = 30L * 24 * 60 * 60;
    private static final long DEFAULT_REFRESH_SECONDS = 7L * 24 * 60 * 60;

    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IssuedSession issueSession(UserEntity user, boolean rememberMe, UUID organizationId,
            String ipAddress, String userAgent) {
        return issueSession(user, rememberMe, organizationId, false, ipAddress, userAgent);
    }

    @Transactional
    public IssuedSession issuePlatformSession(UserEntity user, String ipAddress, String userAgent) {
        return issueSession(user, false, null, true, ipAddress, userAgent);
    }

    private IssuedSession issueSession(UserEntity user, boolean rememberMe, UUID organizationId,
            boolean platformSession, String ipAddress, String userAgent) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawToken = generateRawToken();
        RefreshSessionEntity session = RefreshSessionEntity.builder()
                .user(user)
                .organizationId(organizationId)
                .platformSession(platformSession)
                .tokenHash(sha256(rawToken))
                .expiresAt(now.plusSeconds(rememberMe ? REMEMBER_ME_SECONDS : DEFAULT_REFRESH_SECONDS))
                .rememberMe(rememberMe)
                .build();
        refreshSessionRepository.save(session);
        return new IssuedSession(rawToken, rememberMe);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshedSession rotate(String rawToken, String ipAddress, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = sha256(rawToken);
        RefreshSessionEntity current = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            revokeFamily(current.getUser().getId());
            throw new InvalidRefreshTokenException();
        }

        String nextRawToken = generateRawToken();
        RefreshSessionEntity next = RefreshSessionEntity.builder()
                .user(current.getUser())
                .organizationId(current.getOrganizationId())
                .platformSession(current.isPlatformSession())
                .tokenHash(sha256(nextRawToken))
                .expiresAt(now.plusSeconds(current.isRememberMe()
                        ? REMEMBER_ME_SECONDS
                        : DEFAULT_REFRESH_SECONDS))
                .rememberMe(current.isRememberMe())
                .build();
        refreshSessionRepository.save(next);

        current.setRevokedAt(now);
        current.setReplacedBy(next.getId());
        refreshSessionRepository.save(current);

        JwtClaims claims = new JwtClaims(
                current.getUser().getId(),
                current.getUser().getName(),
                current.getUser().getEmail(),
                current.getOrganizationId(),
                null,
                java.util.List.of(),
                java.util.List.of(),
                now.toInstant(),
                now.plusSeconds(ACCESS_TOKEN_SECONDS).toInstant(),
                UUID.randomUUID().toString());
        return new RefreshedSession(jwtService.issueAccessToken(claims), nextRawToken,
                current.isRememberMe());
    }

    private void revokeFamily(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        refreshSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId).forEach(session -> {
            session.setRevokedAt(now);
            refreshSessionRepository.save(session);
        });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record RefreshedSession(String accessToken, String refreshToken, boolean rememberMe) {
    }

    public record IssuedSession(String refreshToken, boolean rememberMe) {
    }

    @Getter
    public static class InvalidRefreshTokenException extends AbstractBusinessException {
        public InvalidRefreshTokenException() {
            super("INVALID_CREDENTIALS", "Invalid credentials.");
        }
    }
}
