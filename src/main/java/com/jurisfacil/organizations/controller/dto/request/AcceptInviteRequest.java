package com.jurisfacil.organizations.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(@NotBlank String token, @NotBlank String name,
        @NotBlank @Size(min = 8) String password) {
}
