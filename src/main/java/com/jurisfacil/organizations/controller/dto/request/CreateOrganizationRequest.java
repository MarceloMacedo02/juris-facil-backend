package com.jurisfacil.organizations.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(min = 3, max = 255) String name,
        @NotBlank String cnpjCpf,
        @NotBlank @Email String contactEmail,
        @NotBlank String ownerName,
        @NotBlank @Email String ownerEmail,
        @NotBlank @Pattern(regexp = "BASICO|PROFISSIONAL|ENTERPRISE") String planTier,
        String phone,
        String city,
        String state) {
}
