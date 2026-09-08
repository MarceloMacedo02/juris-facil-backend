package com.jurisfacil.organizations.controller;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.organizations.controller.dto.request.UpdateEntitlementRequest;
import com.jurisfacil.organizations.controller.dto.response.EntitlementResponse;
import com.jurisfacil.organizations.service.AdminEntitlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/organizations/{organizationId}/entitlements")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin entitlements")
public class AdminEntitlementsController {

    private final AdminEntitlementService service;

    @GetMapping
    @Operation(summary = "List organization entitlements")
    public List<EntitlementResponse> list(@PathVariable UUID organizationId) {
        return service.list(organizationId);
    }

    @PutMapping
    @Operation(summary = "Update organization entitlement")
    public EntitlementResponse update(@PathVariable UUID organizationId,
            @Valid @RequestBody UpdateEntitlementRequest request, Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        return service.update(organizationId, request, claims.sub());
    }
}
