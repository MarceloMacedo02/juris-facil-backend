package com.jurisfacil.organizations.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.organizations.controller.dto.request.UpdateOrganizationStatusRequest;
import com.jurisfacil.organizations.controller.dto.response.UpdateOrganizationStatusResponse;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.model.enums.SubscriptionStatus;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.organizations.service.TenantStatusService;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantStatusUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantStatusUpdateService.class);
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantStatusService tenantStatusService;

    public UpdateOrganizationStatusResponse update(UUID organizationId, UpdateOrganizationStatusRequest request) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationStatusException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Organization not found."));
        OrganizationStatus nextStatus = parseStatus(request.status());
        String previousStatus = organization.getStatus().name();
        organization.setStatus(nextStatus);
        organizationRepository.save(organization);

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new OrganizationStatusException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Subscription not found."));
        subscription.setStatus(subscriptionStatus(nextStatus));
        subscriptionRepository.save(subscription);
        tenantStatusService.evict(organizationId);
        LOGGER.info("AUDIT_ORGANIZATION_STATUS_CHANGED organizationId={} previousStatus={} currentStatus={} reason={}",
                organizationId, previousStatus, nextStatus, request.reason());
        return new UpdateOrganizationStatusResponse(organizationId, previousStatus, nextStatus.name(),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private OrganizationStatus parseStatus(String value) {
        try {
            return OrganizationStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new OrganizationStatusException(ErrorCode.VALIDATION_ERROR, "Invalid organization status.");
        }
    }

    private SubscriptionStatus subscriptionStatus(OrganizationStatus status) {
        return switch (status) {
            case ACTIVE -> SubscriptionStatus.ACTIVE;
            case SUSPENDED -> SubscriptionStatus.SUSPENDED;
            case INACTIVE -> SubscriptionStatus.CANCELLED;
        };
    }

    public static final class OrganizationStatusException extends AbstractBusinessException {
        public OrganizationStatusException(ErrorCode code, String detail) {
            super(code.name(), detail);
        }
    }
}
