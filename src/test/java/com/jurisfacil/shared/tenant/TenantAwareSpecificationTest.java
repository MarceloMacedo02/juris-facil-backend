package com.jurisfacil.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class TenantAwareSpecificationTest {

    @AfterEach
    void cleanContext() {
        TenantContextHolder.clear();
    }

    @Test
    void createsPredicateFromCurrentTenantOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(organizationId, "LAWYER", java.util.List.of()));
        Root<TenantAwareEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<UUID> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.<UUID>get("organizationId")).thenReturn(path);
        when(criteriaBuilder.equal(path, organizationId)).thenReturn(predicate);

        Specification<TenantAwareEntity> specification = TenantAwareSpecification.byCurrentTenant();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(predicate);
        verify(root).get(eq("organizationId"));
        verify(criteriaBuilder).equal(path, organizationId);
    }

    private static final class TenantAwareEntity implements TenantAware {
        @Override
        public UUID getOrganizationId() {
            return null;
        }
    }
}
