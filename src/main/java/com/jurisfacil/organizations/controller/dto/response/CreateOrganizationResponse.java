package com.jurisfacil.organizations.controller.dto.response;

import java.util.UUID;

public record CreateOrganizationResponse(
        UUID organizationId,
        UUID ownerMembershipId,
        String activationToken) {
}
