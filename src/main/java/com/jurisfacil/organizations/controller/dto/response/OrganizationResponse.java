package com.jurisfacil.organizations.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String cnpjCpf,
        String contactEmail,
        String phone,
        String city,
        String state,
        String status,
        OffsetDateTime createdAt) {
}
