package com.jurisfacil.iam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jurisfacil.iam.model.entity.RefreshSessionEntity;
import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.RefreshSessionRepository;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.shared.email.EmailGateway;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class RecoveryServiceTest {

    private UserRepository userRepository;
    private RefreshSessionRepository refreshSessionRepository;
    private EmailGateway emailGateway;
    private RecoveryService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshSessionRepository = mock(RefreshSessionRepository.class);
        emailGateway = mock(EmailGateway.class);
        service = new RecoveryService(userRepository, refreshSessionRepository, encoder, emailGateway);
    }

    @Test
    void requestStoresOpaqueSha256TokenWithThirtyMinuteTtlAndSendsEmail() {
        UserEntity user = user("person@example.com");
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        service.request("person@example.com");

        assertThat(user.getResetTokenHash()).hasSize(64).doesNotContain("token");
        assertThat(user.getResetTokenExpiresAt()).isBetween(
                OffsetDateTime.now().plusMinutes(29), OffsetDateTime.now().plusMinutes(31));
        verify(userRepository).save(user);
        verify(emailGateway).send(eq(user.getEmail()), eq("Recuperação de senha"), contains("/login/reset?token="));
    }

    @Test
    void requestDoesNotRevealUnknownEmail() {
        service.request("unknown@example.com");

        verifyNoInteractions(emailGateway);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetUpdatesPasswordConsumesTokenAndRevokesActiveSessions() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user("person@example.com");
        user.setId(userId);
        user.setResetTokenHash(RecoveryService.sha256("raw-token"));
        user.setResetTokenExpiresAt(OffsetDateTime.now().plusMinutes(30));
        RefreshSessionEntity session = RefreshSessionEntity.builder().user(user).build();
        when(userRepository.findByResetTokenHash(user.getResetTokenHash())).thenReturn(Optional.of(user));
        when(refreshSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId)).thenReturn(List.of(session));

        service.reset("raw-token", "new-password");

        assertThat(encoder.matches("new-password", user.getPasswordHash())).isTrue();
        assertThat(user.getResetTokenHash()).isNull();
        assertThat(user.getResetTokenExpiresAt()).isNull();
        assertThat(user.getPasswordSetAt()).isNotNull();
        assertThat(session.getRevokedAt()).isNotNull();
        verify(refreshSessionRepository).save(session);
    }

    @Test
    void resetRejectsMissingExpiredOrUnknownToken() {
        when(userRepository.findByResetTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("bad", "new-password"))
                .isInstanceOf(RecoveryService.InvalidResetTokenException.class);
        assertThatThrownBy(() -> service.reset("", "new-password"))
                .isInstanceOf(RecoveryService.InvalidResetTokenException.class);
    }

    private static UserEntity user(String email) {
        return UserEntity.builder().id(UUID.randomUUID()).name("Person").email(email).build();
    }
}
