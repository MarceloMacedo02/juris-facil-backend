package com.jurisfacil.organizations.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(@NotBlank @Pattern(regexp = "ADMIN|LAWYER|ASSISTANT") String role) {
}
