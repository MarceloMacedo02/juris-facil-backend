package com.jurisfacil.organizations.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.organizations.controller.dto.response.OrganizationResponse;
import com.jurisfacil.organizations.service.OrganizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
@Tag(name = "Workspace organization")
public class WorkspaceOrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return the authenticated workspace organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspace organization"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Workspace organization is not available"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    public OrganizationResponse get(Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        if (claims.organizationId() == null) {
            throw new AccessDeniedException("Workspace organization is required");
        }
        return organizationService.getWorkspaceOrganization(claims.organizationId());
    }
}
