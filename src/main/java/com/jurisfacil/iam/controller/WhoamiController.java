package com.jurisfacil.iam.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.security.JwtClaims;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/_dev")
@RequiredArgsConstructor
@Tag(name = "IAM development diagnostics")
public class WhoamiController {

    private final UserRepository userRepository;

    @GetMapping("/whoami")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return the authenticated workspace user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    public WhoamiResponse whoami(Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        UserEntity user = userRepository.findById(claims.sub())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return WhoamiResponse.from(user, claims);
    }

    public record WhoamiResponse(UUID id, String name, String email, String role,
            List<String> entitlements, UUID organizationId) {
        static WhoamiResponse from(UserEntity user, JwtClaims claims) {
            return new WhoamiResponse(user.getId(), user.getName(), user.getEmail(),
                    claims.role(), claims.entitlements(), claims.organizationId());
        }
    }
}
