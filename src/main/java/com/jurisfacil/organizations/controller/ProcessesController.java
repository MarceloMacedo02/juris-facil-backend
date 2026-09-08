package com.jurisfacil.organizations.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.service.EntitlementService;
import com.jurisfacil.processes.controller.dto.response.PageResponse;
import com.jurisfacil.processes.controller.dto.response.ProcessListItem;
import com.jurisfacil.processes.controller.dto.response.ProcessDetailResponse;
import com.jurisfacil.processes.controller.dto.request.CreateProcessRequest;
import com.jurisfacil.processes.controller.dto.request.UpdateProcessRequest;
import com.jurisfacil.processes.controller.dto.request.ProcessStatusRequest;
import com.jurisfacil.processes.controller.dto.response.ProcessResponse;
import com.jurisfacil.processes.controller.dto.response.MovementResponse;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.service.ProcessCreateService;
import com.jurisfacil.processes.service.ProcessListService;
import com.jurisfacil.processes.service.ProcessDetailService;
import com.jurisfacil.processes.service.ProcessUpdateService;
import com.jurisfacil.processes.service.MovementListService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
@Tag(name = "Processes")
public class ProcessesController {

    private final EntitlementService entitlementService;
    private final ProcessListService processListService;
    private final ProcessCreateService processCreateService;
    private final ProcessDetailService processDetailService;
    private final ProcessUpdateService processUpdateService;
    private final MovementListService movementListService;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'LAWYER')")
    @Operation(summary = "Create a process in the workspace")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Process created"),
            @ApiResponse(responseCode = "400", description = "Invalid process data"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Insufficient role or module disabled"),
            @ApiResponse(responseCode = "409", description = "CNJ already exists")
    })
    public ResponseEntity<ProcessResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateProcessRequest request) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        ProcessResponse response = processCreateService.create(
                claims.organizationId(), claims.sub(), request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return process details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Process details"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "Process not found")
    })
    public ProcessDetailResponse detail(@PathVariable UUID id) {
        return processDetailService.get(id);
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List process movements in reverse chronological order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged movement timeline"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Process not found"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    public PageResponse<MovementResponse> movements(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        return movementListService.list(claims.organizationId(), id, page, size);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'LAWYER')")
    @Operation(summary = "Update process metadata and parties")
    public ProcessResponse update(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateProcessRequest request) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        return processUpdateService.update(claims.organizationId(), claims.sub(), id,
                processUpdateService.parseIfMatch(ifMatch), request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'LAWYER')")
    @Operation(summary = "Change process status")
    public ProcessResponse status(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody ProcessStatusRequest request) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        return processUpdateService.changeStatus(claims.organizationId(), claims.sub(), id, request);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'LAWYER')")
    @Operation(summary = "Archive process")
    public ProcessResponse archive(Authentication authentication, @PathVariable UUID id) {
        JwtClaims claims = (JwtClaims) authentication.getPrincipal();
        return processUpdateService.archive(claims.organizationId(), claims.sub(), id);
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
