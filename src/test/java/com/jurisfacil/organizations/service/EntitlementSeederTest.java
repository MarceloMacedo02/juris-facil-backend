package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;

@ExtendWith(MockitoExtension.class)
class EntitlementSeederTest {

    @Mock ModuleEntitlementRepository entitlementRepository;
    private EntitlementSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new EntitlementSeeder(entitlementRepository);
        when(entitlementRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void seedsProfessionalPlanWithOnlyContractedModulesEnabled() {
        List<ModuleEntitlementEntity> result = seeder.seed(UUID.randomUUID(), PlanTier.PROFISSIONAL);

        assertThat(result).hasSize(8);
        assertThat(result.stream().filter(ModuleEntitlementEntity::isEnabled)
                .map(ModuleEntitlementEntity::getModuleCode))
                .containsExactlyInAnyOrder("PROCESS", "DEADLINES", "DOCUMENTS", "TASKS");
    }

    @Test
    void seedsBasicAndEnterprisePlansAccordingToCatalog() {
        List<ModuleEntitlementEntity> basic = seeder.seed(UUID.randomUUID(), PlanTier.BASICO);
        List<ModuleEntitlementEntity> enterprise = seeder.seed(UUID.randomUUID(), PlanTier.ENTERPRISE);

        assertThat(basic).filteredOn(ModuleEntitlementEntity::isEnabled)
                .extracting(ModuleEntitlementEntity::getModuleCode).containsExactly("PROCESS");
        assertThat(enterprise).allMatch(ModuleEntitlementEntity::isEnabled);
    }
}
