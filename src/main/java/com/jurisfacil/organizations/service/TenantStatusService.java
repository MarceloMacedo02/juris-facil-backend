package com.jurisfacil.organizations.service;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.repository.OrganizationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantStatusService {

    private final OrganizationRepository organizationRepository;

    @Cacheable(cacheNames = "tenant-status", key = "#organizationId")
    public boolean isActive(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElse(false);
    }

    @CacheEvict(cacheNames = "tenant-status", key = "#organizationId")
    public void evict(UUID organizationId) {
        // Cache eviction is intentionally handled by the annotation.
    }
}
