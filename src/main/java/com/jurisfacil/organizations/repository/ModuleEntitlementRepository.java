package com.jurisfacil.organizations.repository;

import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleEntitlementRepository extends JpaRepository<ModuleEntitlementEntity, UUID> {

    Optional<ModuleEntitlementEntity> findByOrganizationIdAndModuleCode(
            UUID organizationId, String moduleCode);

    List<ModuleEntitlementEntity> findByOrganizationId(UUID organizationId);
}
