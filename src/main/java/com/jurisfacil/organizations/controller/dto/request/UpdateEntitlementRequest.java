package com.jurisfacil.organizations.controller.dto.request;

import com.jurisfacil.organizations.model.enums.ModuleCode;
import jakarta.validation.constraints.NotNull;

public record UpdateEntitlementRequest(@NotNull ModuleCode moduleCode, boolean enabled) {
}
