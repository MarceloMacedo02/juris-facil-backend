package com.jurisfacil.iam.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.shared.exception.AbstractBusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAuthService {

    private static final long ACCESS_TOKEN_SECONDS = 900;

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshService refreshService;

    public AuthenticatedAdmin login(String email, String password, String ipAddress, String userAgent) {
        UserEntity user;
        try {
            user = authService.authenticate(email, password);
        } catch (RuntimeException exception) {
            throw new InvalidCredentialsException();
        }
        if (user.getPlatformRole() == null) {
            throw new InvalidCredentialsException();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String accessToken = jwtService.issueAccessToken(new JwtClaims(
                user.getId(), user.getName(), user.getEmail(), null, null,
                List.of(user.getPlatformRole().name()), List.of(), now.toInstant(),
                now.plusSeconds(ACCESS_TOKEN_SECONDS).toInstant(), UUID.randomUUID().toString()));
        RefreshService.IssuedSession session = refreshService.issuePlatformSession(user, ipAddress, userAgent);
        return new AuthenticatedAdmin(user, accessToken, session.refreshToken());
    }

    public record AuthenticatedAdmin(UserEntity user, String accessToken, String refreshToken) {
    }

    public static class InvalidCredentialsException extends AbstractBusinessException {
        public InvalidCredentialsException() {
            super("INVALID_CREDENTIALS", "Invalid credentials.");
        }
    }
}
