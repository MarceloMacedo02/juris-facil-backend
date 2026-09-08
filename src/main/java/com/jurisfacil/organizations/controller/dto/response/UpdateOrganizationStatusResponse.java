package com.jurisfacil.organizations.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateOrganizationStatusResponse(
        UUID organizationId,
        String previousStatus,
        String currentStatus,
        OffsetDateTime updatedAt) {
}
