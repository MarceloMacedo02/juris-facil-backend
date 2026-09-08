package com.jurisfacil.iam.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.service.AdminAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Validated
@Tag(name = "Admin Authentication")
public class AdminAuthController {

        static final String REFRESH_COOKIE = "JF_ADMIN_REFRESH";
        private final AdminAuthService adminAuthService;

        public AdminAuthController(AdminAuthService adminAuthService) {
                this.adminAuthService = adminAuthService;
        }

        @PostMapping("/login")
        @Operation(summary = "Authenticate platform administrator")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Authenticated"),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
                        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
                        @ApiResponse(responseCode = "500", description = "Internal error")
        })
        public ResponseEntity<LoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                        HttpServletRequest httpRequest) {
                AdminAuthService.AuthenticatedAdmin authenticated = adminAuthService.login(
                                request.email(), request.password(), httpRequest.getRemoteAddr(),
                                httpRequest.getHeader(HttpHeaders.USER_AGENT));
                ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, authenticated.refreshToken())
                                .httpOnly(true).secure(true).sameSite("Lax").path("/api/admin")
                                .maxAge(Duration.ofDays(7)).build();
                return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(LoginResponse.from(authenticated));
        }

        public record AdminLoginRequest(@Email @NotBlank String email,
                        @NotBlank @Size(min = 8) String password) {
        }

        public record LoginResponse(String access_token, String token_type, long expires_in,
                        String name, String email, java.util.List<String> roles) {
                static LoginResponse from(AdminAuthService.AuthenticatedAdmin authenticated) {
                        return new LoginResponse(authenticated.accessToken(), "Bearer", 900,
                                        authenticated.user().getName(), authenticated.user().getEmail(),
                                        java.util.List.of(authenticated.user().getPlatformRole().name()));
                }
        }
}
