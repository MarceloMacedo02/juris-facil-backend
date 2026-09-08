package com.jurisfacil.iam.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.PlatformRole;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserEntityPersistenceIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsUserWithV1ColumnMappingsAndAuditingTimestamps() {
        OffsetDateTime passwordSetAt = OffsetDateTime.now().minusMinutes(1);
        OffsetDateTime resetExpiresAt = OffsetDateTime.now().plusHours(1);
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .name("Identity User")
                .email("entity." + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$12$identity-hash")
                .status(UserStatus.ACTIVE)
                .platformRole(PlatformRole.SUPPORT)
                .resetTokenHash("f".repeat(64))
                .resetTokenExpiresAt(resetExpiresAt)
                .passwordSetAt(passwordSetAt)
                .build());

        UserEntity persisted = userRepository.findById(user.getId()).orElseThrow();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(persisted.getPlatformRole()).isEqualTo(PlatformRole.SUPPORT);
        assertThat(Duration.between(resetExpiresAt, persisted.getResetTokenExpiresAt()).abs())
                .isLessThan(Duration.ofSeconds(1));
        assertThat(Duration.between(passwordSetAt, persisted.getPasswordSetAt()).abs())
                .isLessThan(Duration.ofSeconds(1));
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }

    @Test
    void updatesUpdatedAtWhenUserChanges() {
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .name("Before Update")
                .email("update." + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$12$identity-hash")
                .status(UserStatus.INACTIVE)
                .build());
        OffsetDateTime createdAt = user.getCreatedAt();
        OffsetDateTime updatedAt = user.getUpdatedAt();

        user.setName("After Update");
        UserEntity updated = userRepository.saveAndFlush(user);

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
    }
}
