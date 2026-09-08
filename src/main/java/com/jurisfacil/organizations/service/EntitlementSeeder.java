package com.jurisfacil.organizations.service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntitlementSeeder {

    private final ModuleEntitlementRepository entitlementRepository;

    @Transactional
    public List<ModuleEntitlementEntity> seed(UUID organizationId, PlanTier planTier) {
        Set<ModuleCode> enabledModules = enabledModulesFor(planTier);
        List<ModuleEntitlementEntity> entitlements = Arrays.stream(ModuleCode.values())
                .filter(module -> module != ModuleCode.CORE)
                .map(module -> ModuleEntitlementEntity.builder()
                        .organizationId(organizationId)
                        .moduleCode(module.name())
                        .enabled(enabledModules.contains(module))
                        .build())
                .toList();
        return entitlementRepository.saveAll(entitlements);
    }

    private Set<ModuleCode> enabledModulesFor(PlanTier planTier) {
        return switch (planTier) {
            case BASICO -> EnumSet.of(ModuleCode.PROCESS);
            case PROFISSIONAL -> EnumSet.of(
                    ModuleCode.PROCESS, ModuleCode.DEADLINES, ModuleCode.DOCUMENTS, ModuleCode.TASKS);
            case ENTERPRISE -> EnumSet.complementOf(EnumSet.of(ModuleCode.CORE));
        };
    }
}
