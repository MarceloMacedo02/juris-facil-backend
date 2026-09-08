package com.jurisfacil.iam.controller;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.service.AuthService;
import com.jurisfacil.iam.service.RefreshService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Workspace Authentication")
public class WorkspaceAuthController {

        static final String REFRESH_COOKIE = "JF_REFRESH";

        private final RefreshService refreshService;
        private final AuthService authService;

        public WorkspaceAuthController(RefreshService refreshService, AuthService authService) {
                this.refreshService = refreshService;
                this.authService = authService;
        }

        @PostMapping("/login")
        @Operation(summary = "Authenticate workspace user")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Authenticated"),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
                        @ApiResponse(responseCode = "403", description = "Account inactive"),
                        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
                        @ApiResponse(responseCode = "500", description = "Internal error")
        })
        public ResponseEntity<LoginResponse> login(
                        @Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest) {
                AuthService.AuthenticatedUser authenticated = authService.login(
                                request.email(),
                                request.password(),
                                request.rememberMe(),
                                httpRequest.getRemoteAddr(),
                                httpRequest.getHeader(HttpHeaders.USER_AGENT));
                ResponseCookie cookie = refreshCookie(authenticated.refreshToken(), request.rememberMe());
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(LoginResponse.from(authenticated));
        }

        @PostMapping("/refresh")
        @Operation(summary = "Rotate workspace refresh token")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Refresh token rotated"),
                        @ApiResponse(responseCode = "401", description = "Invalid, expired or reused refresh token"),
                        @ApiResponse(responseCode = "403", description = "Tenant suspended"),
                        @ApiResponse(responseCode = "500", description = "Internal error")
        })
        public ResponseEntity<RefreshResponse> refresh(
                        @CookieValue(name = REFRESH_COOKIE, required = false) String rawToken,
                        HttpServletRequest request) {
                RefreshService.RefreshedSession refreshed = refreshService.rotate(
                                rawToken,
                                request.getRemoteAddr(),
                                request.getHeader(HttpHeaders.USER_AGENT));
                ResponseCookie cookie = refreshCookie(refreshed.refreshToken(), refreshed.rememberMe());
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(new RefreshResponse(refreshed.accessToken(), 900, "Bearer"));
        }

        public record RefreshResponse(String access_token, long expires_in, String token_type) {
        }

        private ResponseCookie refreshCookie(String token, boolean rememberMe) {
                return ResponseCookie.from(REFRESH_COOKIE, token)
                                .httpOnly(true)
                                .secure(true)
                                .sameSite("Lax")
                                .path("/api")
                                .maxAge(Duration.ofDays(rememberMe ? 30 : 7))
                                .build();
        }

        public record LoginRequest(
                        @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
                        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 8) String password,
                        boolean remember_me) {
                boolean rememberMe() {
                        return remember_me;
                }
        }

        public record LoginResponse(
                        String access_token,
                        String token_type,
                        long expires_in,
                        LoginUser user) {
                static LoginResponse from(AuthService.AuthenticatedUser authenticated) {
                        return new LoginResponse(authenticated.accessToken(), "Bearer", 900,
                                        new LoginUser(authenticated.user().getId(), authenticated.user().getName(),
                                                        authenticated.user().getEmail(), null, null, List.of(), null));
                }
        }

        public record LoginUser(
                        UUID user_id,
                        String name,
                        String email,
                        String role,
                        UUID organization_id,
                        List<String> entitlements,
                        String plan_tier) {
        }
}
