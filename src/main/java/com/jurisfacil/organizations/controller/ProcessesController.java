package com.jurisfacil.organizations.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.service.EntitlementService;
import com.jurisfacil.processes.controller.dto.response.PageResponse;
import com.jurisfacil.processes.controller.dto.response.ProcessListItem;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.service.ProcessListService;

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
    private final ProcessListService processListService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List processes available to the workspace")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged process list"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Process module is disabled"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    public PageResponse<ProcessListItem> list(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProcessStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        UUID organizationId = claims.organizationId();
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        return processListService.list(q, status, page, size);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return workspace process summary")
    public ProcessSummaryResponse summary(Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        entitlementService.assertEnabled(claims.organizationId(), ModuleCode.PROCESS);
        return new ProcessSummaryResponse(0, 0, 0, 0);
    }

    public record ProcessSummaryResponse(
            int activeProcesses,
            int urgentDeadlines,
            int upcomingHearings,
            int documentsCount) {
    }
}
