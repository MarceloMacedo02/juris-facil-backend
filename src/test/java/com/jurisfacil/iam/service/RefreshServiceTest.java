package com.jurisfacil.iam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.iam.model.entity.RefreshSessionEntity;
import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.RefreshSessionRepository;
import com.jurisfacil.iam.security.JwtService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshServiceTest {

    private static final String JWT_SECRET = "jurisfacil-unit-test-secret-32-bytes!!";

    @Mock
    private RefreshSessionRepository repository;

    private RefreshService service;

    @BeforeEach
    void setUp() {
        service = new RefreshService(repository, new JwtService(JWT_SECRET));
    }

    @Test
    void rotatesActiveTokenAndStoresOnlySha256Hash() {
        UUID userId = UUID.randomUUID();
        RefreshSessionEntity current = session(userId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), false);
        when(repository.findByTokenHash(RefreshService.sha256("raw-token"))).thenReturn(Optional.of(current));
        when(repository.save(any(RefreshSessionEntity.class))).thenAnswer(invocation -> {
            RefreshSessionEntity saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        RefreshService.RefreshedSession result = service.rotate("raw-token", "127.0.0.1", "JUnit");

        assertThat(result.refreshToken()).hasSize(43).isNotEqualTo("raw-token");
        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(current.getReplacedBy()).isNotNull();
        verify(repository).save(current);
        assertThat(result.accessToken()).isNotBlank();
    }

    @Test
    void reusingRevokedTokenRevokesAllActiveSessions() {
        UUID userId = UUID.randomUUID();
        RefreshSessionEntity revoked = session(userId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), false);
        revoked.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        RefreshSessionEntity active = session(userId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), false);
        when(repository.findByTokenHash(RefreshService.sha256("reused"))).thenReturn(Optional.of(revoked));
        when(repository.findAllByUser_IdAndRevokedAtIsNull(userId)).thenReturn(List.of(active));

        assertThatThrownBy(() -> service.rotate("reused", "127.0.0.1", "JUnit"))
                .isInstanceOf(RefreshService.InvalidRefreshTokenException.class);

        assertThat(active.getRevokedAt()).isNotNull();
        verify(repository).save(active);
    }

    @Test
    void expiredTokenRevokesFamilyAndFails() {
        UUID userId = UUID.randomUUID();
        RefreshSessionEntity expired = session(userId, OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1), false);
        when(repository.findByTokenHash(RefreshService.sha256("expired"))).thenReturn(Optional.of(expired));
        when(repository.findAllByUser_IdAndRevokedAtIsNull(userId)).thenReturn(List.of(expired));

        assertThatThrownBy(() -> service.rotate("expired", "127.0.0.1", "JUnit"))
                .isInstanceOf(RefreshService.InvalidRefreshTokenException.class);

        assertThat(expired.getRevokedAt()).isNotNull();
        verify(repository).save(expired);
    }

    private static RefreshSessionEntity session(UUID userId, OffsetDateTime expiresAt, boolean rememberMe) {
        UserEntity user = UserEntity.builder()
                .id(userId)
                .name("Refresh User")
                .email("refresh@example.com")
                .build();
        return RefreshSessionEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("stored-hash")
                .expiresAt(expiresAt)
                .rememberMe(rememberMe)
                .build();
    }
}
