package com.jurisfacil.organizations.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InviteMemberRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "ADMIN|LAWYER|ASSISTANT") String role) {
}
