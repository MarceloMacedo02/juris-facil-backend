package com.jurisfacil.processes.repository;

import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessRepository extends JpaRepository<ProcessEntity, UUID>, JpaSpecificationExecutor<ProcessEntity> {

    boolean existsByOrganizationIdAndCnjNumberAndStatusNot(UUID organizationId, String cnjNumber,
            ProcessStatus status);
}
