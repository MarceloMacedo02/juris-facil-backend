package com.jurisfacil.iam.service;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.RefreshSessionRepository;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.shared.email.EmailGateway;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RecoveryService {

    static final int TOKEN_BYTES = 32;
    static final int TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailGateway emailGateway;
    private final SecureRandom secureRandom;

    @Autowired
    public RecoveryService(UserRepository userRepository, RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder, EmailGateway emailGateway) {
        this(userRepository, refreshSessionRepository, passwordEncoder, emailGateway, new SecureRandom());
    }

    RecoveryService(UserRepository userRepository, RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder, EmailGateway emailGateway, SecureRandom secureRandom) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailGateway = emailGateway;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public void request(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = generateToken();
            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(TOKEN_TTL_MINUTES);
            user.setResetTokenHash(sha256(rawToken));
            user.setResetTokenExpiresAt(expiresAt);
            userRepository.save(user);
            emailGateway.send(user.getEmail(), "Recuperação de senha",
                    "/login/reset?token=" + rawToken);
        });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidResetTokenException();
        }
        UserEntity user = userRepository.findByResetTokenHash(sha256(rawToken))
                .orElseThrow(InvalidResetTokenException::new);
        OffsetDateTime expiresAt = user.getResetTokenExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new InvalidResetTokenException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
        user.setPasswordSetAt(OffsetDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        refreshSessionRepository.findAllByUser_IdAndRevokedAtIsNull(user.getId()).forEach(session -> {
            session.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
            refreshSessionRepository.save(session);
        });
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static class InvalidResetTokenException extends AbstractBusinessException {
        public InvalidResetTokenException() {
            super("RESET_TOKEN_INVALID", "Link inválido ou expirado. Solicite um novo.");
        }
    }
}
