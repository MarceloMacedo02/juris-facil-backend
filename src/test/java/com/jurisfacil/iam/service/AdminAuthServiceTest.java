package com.jurisfacil.iam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.PlatformRole;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.iam.service.impl.AuthServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {
    @Mock private AuthService authService;
    @Mock private JwtService jwtService;
    @Mock private RefreshService refreshService;
    private AdminAuthService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuthService(authService, jwtService, refreshService);
    }

    @Test
    void issuesPlatformTokenAndPlatformRefreshSession() {
        UserEntity user = user(PlatformRole.PLATFORM_ADMIN);
        when(authService.authenticate(user.getEmail(), "password")).thenReturn(user);
        when(jwtService.issueAccessToken(any())).thenReturn("admin-access");
        when(refreshService.issuePlatformSession(user, "ip", "agent"))
                .thenReturn(new RefreshService.IssuedSession("admin-refresh", false));

        AdminAuthService.AuthenticatedAdmin result = service.login(user.getEmail(), "password", "ip", "agent");

        assertThat(result.accessToken()).isEqualTo("admin-access");
        assertThat(result.refreshToken()).isEqualTo("admin-refresh");
        verify(jwtService).issueAccessToken(any());
        verify(refreshService).issuePlatformSession(user, "ip", "agent");
    }

    @Test
    void rejectsUserWithoutPlatformRoleWithGenericCredentialsError() {
        UserEntity user = user(null);
        when(authService.authenticate(user.getEmail(), "password")).thenReturn(user);

        assertThatThrownBy(() -> service.login(user.getEmail(), "password", "ip", "agent"))
                .isInstanceOf(AdminAuthService.InvalidCredentialsException.class);
    }

    @Test
    void mapsCredentialFailureToGenericAdminError() {
        when(authService.authenticate("admin@example.com", "wrong"))
                .thenThrow(new AuthServiceImpl.InvalidCredentialsException());

        assertThatThrownBy(() -> service.login("admin@example.com", "wrong", "ip", "agent"))
                .isInstanceOf(AdminAuthService.InvalidCredentialsException.class);
    }

    private static UserEntity user(PlatformRole role) {
        return UserEntity.builder().id(UUID.randomUUID()).name("Platform User")
                .email(UUID.randomUUID() + "@example.com").platformRole(role)
                .passwordHash("hash").status(com.jurisfacil.iam.model.enums.UserStatus.ACTIVE).build();
    }
}
