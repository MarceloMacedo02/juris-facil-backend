package com.jurisfacil.shared.tenant;

import java.util.UUID;

public interface TenantAware {

    UUID getOrganizationId();
}
