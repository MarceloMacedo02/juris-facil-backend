package com.jurisfacil.iam.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.iam.model.entity.RefreshSessionEntity;
import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class RefreshSessionRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Test
    void findsByTokenHashAndOnlyReturnsActiveSessionsForUser() {
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .name("Refresh User")
                .email("refresh." + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$12$identity-hash")
                .status(UserStatus.ACTIVE)
                .build());
        RefreshSessionEntity active = saveSession(user, false, null);
        RefreshSessionEntity revoked = saveSession(user, false, OffsetDateTime.now());

        assertThat(refreshSessionRepository.findByTokenHash(active.getTokenHash()))
                .get()
                .extracting(RefreshSessionEntity::getId)
                .isEqualTo(active.getId());
        assertThat(refreshSessionRepository.findAllByUser_IdAndRevokedAtIsNull(user.getId()))
                .extracting(RefreshSessionEntity::getId)
                .containsExactly(active.getId())
                .doesNotContain(revoked.getId());
    }

    @Test
    void persistsSessionFieldsUsingSessionIdAndRememberMeColumns() {
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .name("Platform User")
                .email("platform." + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$12$identity-hash")
                .status(UserStatus.ACTIVE)
                .build());
        UUID organizationId = UUID.randomUUID();
        RefreshSessionEntity session = refreshSessionRepository.saveAndFlush(RefreshSessionEntity.builder()
                .user(user)
                .organizationId(organizationId)
                .platformSession(true)
                .tokenHash(UUID.randomUUID().toString().replace("-", "").repeat(2))
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .rememberMe(true)
                .build());

        RefreshSessionEntity persisted = refreshSessionRepository.findById(session.getId()).orElseThrow();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getOrganizationId()).isEqualTo(organizationId);
        assertThat(persisted.isPlatformSession()).isTrue();
        assertThat(persisted.isRememberMe()).isTrue();
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    private RefreshSessionEntity saveSession(
            UserEntity user, boolean rememberMe, OffsetDateTime revokedAt) {
        return refreshSessionRepository.saveAndFlush(RefreshSessionEntity.builder()
                .user(user)
                .tokenHash(UUID.randomUUID().toString().replace("-", "").repeat(2))
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .rememberMe(rememberMe)
                .revokedAt(revokedAt)
                .build());
    }
}
