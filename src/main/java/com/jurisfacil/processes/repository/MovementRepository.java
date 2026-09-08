package com.jurisfacil.processes.repository;

import com.jurisfacil.processes.model.entity.ProcessMovementEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovementRepository extends JpaRepository<ProcessMovementEntity, UUID>,
        JpaSpecificationExecutor<ProcessMovementEntity> {

    Optional<ProcessMovementEntity> findFirstByProcessIdAndOrganizationIdOrderByMovementDateDesc(
            UUID processId, UUID organizationId);

    Page<ProcessMovementEntity> findByOrganizationIdAndProcessIdOrderByMovementDateDesc(
            UUID organizationId, UUID processId, Pageable pageable);
}
