package com.jurisfacil.organizations.controller.dto.response;

import com.jurisfacil.organizations.model.enums.ModuleCode;

public record EntitlementResponse(ModuleCode moduleCode, boolean enabled) {
}
