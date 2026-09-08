package com.jurisfacil.organizations.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.organizations.controller.dto.request.CreateOrganizationRequest;
import com.jurisfacil.organizations.controller.dto.response.CreateOrganizationResponse;
import com.jurisfacil.organizations.service.AdminOrganizationService;

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
public class AdminOrganizationsController {

    private final AdminOrganizationService adminOrganizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an organization with its first owner")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Organization created"),
            @ApiResponse(responseCode = "403", description = "Platform administrator role required"),
            @ApiResponse(responseCode = "409", description = "Email or CNPJ/CPF already in use"),
            @ApiResponse(responseCode = "422", description = "Invalid CNPJ/CPF"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public CreateOrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return adminOrganizationService.createOrganization(request);
    }
}
