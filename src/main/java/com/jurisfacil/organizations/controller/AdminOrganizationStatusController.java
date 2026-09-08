package com.jurisfacil.organizations.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.organizations.controller.dto.request.UpdateOrganizationStatusRequest;
import com.jurisfacil.organizations.controller.dto.response.UpdateOrganizationStatusResponse;
import com.jurisfacil.organizations.service.impl.TenantStatusUpdateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Admin organizations")
public class AdminOrganizationStatusController {

    private final TenantStatusUpdateService tenantStatusUpdateService;

    @PutMapping("/{organizationId}/status")
    @Operation(summary = "Suspend or reactivate an organization")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Organization status updated"),
            @ApiResponse(responseCode = "403", description = "Platform administrator role required"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "422", description = "Invalid status"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UpdateOrganizationStatusResponse update(@PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationStatusRequest request) {
        return tenantStatusUpdateService.update(organizationId, request);
    }
}
