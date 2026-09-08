package com.jurisfacil.organizations.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrganizationStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED|INACTIVE") String status,
        String reason) {
}
