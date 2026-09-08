package com.jurisfacil.iam.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.PlatformRole;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.iam.service.impl.AuthServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshService refreshService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, refreshService);
    }

    @Test
    void authenticatesActiveUserAndIssuesSession() {
        UserEntity user = user(UserStatus.ACTIVE, null, "hash");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(jwtService.issueAccessToken(any())).thenReturn("access");
        when(refreshService.issueSession(user, true, null, "ip", "agent"))
                .thenReturn(new RefreshService.IssuedSession("refresh", true));

        AuthService.AuthenticatedUser result = service.login(user.getEmail(), "password", true, "ip", "agent");

        org.assertj.core.api.Assertions.assertThat(result.accessToken()).isEqualTo("access");
        org.assertj.core.api.Assertions.assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(refreshService).issueSession(user, true, null, "ip", "agent");
    }

    @Test
    void rejectsUnknownUserWithoutPasswordCheck() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("unknown@example.com", "password", false, "ip", "agent"))
                .isInstanceOf(AuthServiceImpl.InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void rejectsNullPasswordHashAsInvalidCredentials() {
        UserEntity user = user(UserStatus.ACTIVE, null, null);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(user.getEmail(), "password", false, "ip", "agent"))
                .isInstanceOf(AuthServiceImpl.InvalidCredentialsException.class);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void rejectsInactiveUserWithAccessDenied() {
        UserEntity user = user(UserStatus.INACTIVE, null, "hash");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(user.getEmail(), "password", false, "ip", "agent"))
                .isInstanceOf(AuthServiceImpl.AccessDeniedException.class);
    }

    @Test
    void rejectsPlatformUserFromWorkspaceLogin() {
        UserEntity user = user(UserStatus.ACTIVE, PlatformRole.PLATFORM_ADMIN, "hash");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(user.getEmail(), "password", false, "ip", "agent"))
                .isInstanceOf(AuthServiceImpl.InvalidCredentialsException.class);
        verify(refreshService, never()).issueSession(any(), any(boolean.class), any(), any(), any());
    }

    private static UserEntity user(UserStatus status, PlatformRole platformRole, String passwordHash) {
        return UserEntity.builder().id(UUID.randomUUID()).name("Test User")
                .email(UUID.randomUUID() + "@example.com").passwordHash(passwordHash)
                .status(status).platformRole(platformRole).build();
    }
}
