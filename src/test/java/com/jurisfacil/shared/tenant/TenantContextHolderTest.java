package com.jurisfacil.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextHolderTest {

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
    }

    @Test
    void setsGetsAndClearsContextOnCurrentThread() {
        UUID organizationId = UUID.randomUUID();
        TenantContext context = new TenantContext(organizationId, "LAWYER", List.of("PROCESS"));

        TenantContextHolder.set(context);

        assertThat(TenantContextHolder.get()).isSameAs(context);
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void contextEntitlementsAreImmutable() {
        TenantContext context = new TenantContext(UUID.randomUUID(), "LAWYER", List.of("PROCESS"));

        assertThat(context.entitlements()).containsExactly("PROCESS");
        assertThatThrownBy(() -> context.entitlements().add("CORE"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
