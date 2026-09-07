package com.jurisfacil.shared.tenant;

import java.util.List;
import java.util.UUID;

public record TenantContext(UUID organizationId, String role, List<String> entitlements) {

    public TenantContext {
        entitlements = entitlements == null ? List.of() : List.copyOf(entitlements);
    }
}
