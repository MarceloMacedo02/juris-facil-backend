package com.jurisfacil.iam.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.iam.service.AuthService;
import com.jurisfacil.iam.service.RefreshService;
import com.jurisfacil.shared.exception.AbstractBusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long ACCESS_TOKEN_SECONDS = 900;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshService refreshService;

    @Override
    public UserEntity authenticate(String email, String password) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException();
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            LOGGER.info("AUDIT_LOGIN_FAILED userId={}", user.getId());
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Override
    public AuthenticatedUser login(String email, String password, boolean rememberMe, String ipAddress,
            String userAgent) {
        UserEntity user = authenticate(email, password);
        if (user.getPlatformRole() != null) {
            LOGGER.info("AUDIT_LOGIN_FAILED userId={}", user.getId());
            throw new InvalidCredentialsException();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String accessToken = jwtService.issueAccessToken(new JwtClaims(
                user.getId(), user.getName(), user.getEmail(), null, null, List.of(), List.of(),
                now.toInstant(), now.plusSeconds(ACCESS_TOKEN_SECONDS).toInstant(), UUID.randomUUID().toString()));
        RefreshService.IssuedSession session = refreshService.issueSession(user, rememberMe, null, ipAddress,
                userAgent);
        return new AuthenticatedUser(user, accessToken, session.refreshToken());
    }

    public static class InvalidCredentialsException extends AbstractBusinessException {
        public InvalidCredentialsException() {
            super("INVALID_CREDENTIALS", "Invalid credentials.");
        }
    }

    public static class AccessDeniedException extends AbstractBusinessException {
        public AccessDeniedException() {
            super("ACCESS_DENIED", "Access is denied.");
        }
    }
}
