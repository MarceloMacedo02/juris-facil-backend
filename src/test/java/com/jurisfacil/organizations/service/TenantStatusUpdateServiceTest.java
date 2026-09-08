package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jurisfacil.organizations.controller.dto.request.UpdateOrganizationStatusRequest;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.model.enums.SubscriptionStatus;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.organizations.service.impl.TenantStatusUpdateService;

@ExtendWith(MockitoExtension.class)
class TenantStatusUpdateServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock TenantStatusService tenantStatusService;

    @Test
    void suspendsOrganizationAndSubscriptionAndEvictsTenantCache() {
        UUID organizationId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder().id(organizationId)
                .status(OrganizationStatus.ACTIVE).build();
        SubscriptionEntity subscription = SubscriptionEntity.builder().organizationId(organizationId)
                .status(SubscriptionStatus.ACTIVE).build();
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(subscription));

        var response = new TenantStatusUpdateService(organizationRepository, subscriptionRepository,
                tenantStatusService).update(organizationId,
                        new UpdateOrganizationStatusRequest("SUSPENDED", "fraud review"));

        assertThat(response.previousStatus()).isEqualTo("ACTIVE");
        assertThat(response.currentStatus()).isEqualTo("SUSPENDED");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        verify(organizationRepository).save(organization);
        verify(subscriptionRepository).save(subscription);
        verify(tenantStatusService).evict(organizationId);
    }
}
