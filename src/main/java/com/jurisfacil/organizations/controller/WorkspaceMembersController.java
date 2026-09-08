package com.jurisfacil.organizations.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.organizations.controller.dto.request.InviteMemberRequest;
import com.jurisfacil.organizations.controller.dto.request.UpdateRoleRequest;
import com.jurisfacil.organizations.controller.dto.response.InviteMemberResponse;
import com.jurisfacil.organizations.controller.dto.response.MemberResponse;
import com.jurisfacil.organizations.service.OrganizationsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspace/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
@Tag(name = "Workspace members")
public class WorkspaceMembersController {

    private final OrganizationsService organizationsService;

    @GetMapping
    @Operation(summary = "List workspace members")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Members"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Role not allowed"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public List<MemberResponse> list(Authentication authentication) {
        return organizationsService.listMembers(organization(authentication));
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a workspace member")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Invitation created"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Role or plan not allowed"),
            @ApiResponse(responseCode = "409", description = "Email already in use"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public InviteMemberResponse invite(Authentication authentication, @Valid @RequestBody InviteMemberRequest request) {
        return organizationsService.invite(organization(authentication), request);
    }

    @PatchMapping("/{membershipId}")
    @Operation(summary = "Change a member role")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Member updated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Role not allowed"),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public MemberResponse update(Authentication authentication, @PathVariable UUID membershipId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return organizationsService.updateRole(organization(authentication), membershipId, request);
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a member")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Member deactivated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Role not allowed"),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public void deactivate(Authentication authentication, @PathVariable UUID membershipId) {
        organizationsService.deactivate(organization(authentication), membershipId);
    }

    @PostMapping("/{membershipId}/resend-invite")
    @Operation(summary = "Resend a pending invitation")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Invitation resent"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Role not allowed"),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public MemberResponse resendInvite(Authentication authentication, @PathVariable UUID membershipId) {
        return organizationsService.resendInvite(organization(authentication), membershipId);
    }

    @PostMapping("/{membershipId}/transfer-ownership")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Transfer workspace ownership")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ownership transferred"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Owner role required"),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public MemberResponse transferOwnership(Authentication authentication, @PathVariable UUID membershipId) {
        return organizationsService.transferOwnership(organization(authentication), membershipId);
    }

    private UUID organization(Authentication authentication) {
        return ((JwtClaims) authentication.getPrincipal()).organizationId();
    }
}
