package com.jurisfacil.organizations.service;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final ModuleEntitlementRepository entitlementRepository;

    @Cacheable(cacheNames = "entitlements", key = "#organizationId + ':' + #moduleCode.name()")
    public boolean assertEnabled(UUID organizationId, ModuleCode moduleCode) {
        boolean enabled = entitlementRepository.findByOrganizationIdAndModuleCode(
                        organizationId, moduleCode.name())
                .map(entitlement -> entitlement.isEnabled())
                .orElse(false);
        if (!enabled) {
            throw new ModuleDisabledException(moduleCode);
        }
        return true;
    }

    @CacheEvict(cacheNames = "entitlements", allEntries = true)
    public void evict(UUID organizationId) {
        // Cache eviction is intentionally handled by the annotation.
    }

    public static final class ModuleDisabledException extends AbstractBusinessException {
        public ModuleDisabledException(ModuleCode moduleCode) {
            super(ErrorCode.MODULE_DISABLED.name(), "Module " + moduleCode.name() + " is disabled.");
        }
    }
}
