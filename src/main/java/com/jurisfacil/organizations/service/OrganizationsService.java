package com.jurisfacil.organizations.service;

import java.util.List;
import java.util.UUID;

import com.jurisfacil.organizations.controller.dto.request.InviteMemberRequest;
import com.jurisfacil.organizations.controller.dto.request.UpdateRoleRequest;
import com.jurisfacil.organizations.controller.dto.response.InviteMemberResponse;
import com.jurisfacil.organizations.controller.dto.response.MemberResponse;

public interface OrganizationsService {
    List<MemberResponse> listMembers(UUID organizationId);
    InviteMemberResponse invite(UUID organizationId, InviteMemberRequest request);
    MemberResponse updateRole(UUID organizationId, UUID membershipId, UpdateRoleRequest request);
    void deactivate(UUID organizationId, UUID membershipId);
    MemberResponse resendInvite(UUID organizationId, UUID membershipId);
    MemberResponse transferOwnership(UUID organizationId, UUID membershipId);
}
