package com.jurisfacil.shared.tenant;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class TenantAwareSpecification {

    private TenantAwareSpecification() {
    }

    public static <T extends TenantAware> Specification<T> byCurrentTenant() {
        TenantContext context = TenantContextHolder.get();
        UUID organizationId = context == null ? null : context.organizationId();
        return (root, query, criteriaBuilder) -> organizationId == null
                ? criteriaBuilder.disjunction()
                : criteriaBuilder.equal(root.get("organizationId"), organizationId);
    }
}
