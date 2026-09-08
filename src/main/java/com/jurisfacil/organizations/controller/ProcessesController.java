package com.jurisfacil.organizations.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.service.EntitlementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
@Tag(name = "Processes")
public class ProcessesController {

    private final EntitlementService entitlementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List processes available to the workspace")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged process list"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Process module is disabled"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    public ProcessListResponse list(Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        UUID organizationId = claims.organizationId();
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        return new ProcessListResponse(List.of(), 0, 10, 0, 0);
    }

    public record ProcessListResponse(
            List<Object> items,
            int page,
            int size,
            int totalPages,
            int totalItems) {
    }
}
