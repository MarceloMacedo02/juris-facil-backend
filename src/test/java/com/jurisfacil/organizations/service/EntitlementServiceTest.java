package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;

@SpringBootTest
class EntitlementServiceTest {

    @Autowired EntitlementService entitlementService;
    @Autowired CacheManager cacheManager;
    @MockBean ModuleEntitlementRepository entitlementRepository;

    @Test
    void cachesEnabledModulesAndEvictRefreshesTheValue() {
        UUID organizationId = UUID.randomUUID();
        when(entitlementRepository.findByOrganizationIdAndModuleCode(organizationId, "PROCESS"))
                .thenReturn(java.util.Optional.of(ModuleEntitlementEntity.builder()
                        .organizationId(organizationId)
                        .moduleCode("PROCESS")
                        .enabled(true)
                        .build()))
                .thenReturn(java.util.Optional.empty());
        cacheManager.getCache("entitlements").clear();

        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        verify(entitlementRepository).findByOrganizationIdAndModuleCode(organizationId, "PROCESS");

        entitlementService.evict(organizationId);
        assertThatThrownBy(() -> entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS))
                .isInstanceOf(EntitlementService.ModuleDisabledException.class);
        verify(entitlementRepository, org.mockito.Mockito.times(2))
                .findByOrganizationIdAndModuleCode(organizationId, "PROCESS");
    }
}
