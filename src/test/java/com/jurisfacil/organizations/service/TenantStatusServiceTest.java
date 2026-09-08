package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class TenantStatusServiceTest {

    @Mock OrganizationRepository organizationRepository;

    @Test
    void reportsOnlyActiveOrganizationsAsActive() {
        UUID organizationId = UUID.randomUUID();
        TenantStatusService service = new TenantStatusService(organizationRepository);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(OrganizationEntity.builder()
                .id(organizationId).status(OrganizationStatus.SUSPENDED).build()));

        assertThat(service.isActive(organizationId)).isFalse();
        verify(organizationRepository).findById(organizationId);
    }
}
