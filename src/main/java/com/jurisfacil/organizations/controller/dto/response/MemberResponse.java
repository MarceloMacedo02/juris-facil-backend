package com.jurisfacil.organizations.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(UUID membershipId, UUID userId, String name, String email,
        String role, String status, OffsetDateTime createdAt) {
}
