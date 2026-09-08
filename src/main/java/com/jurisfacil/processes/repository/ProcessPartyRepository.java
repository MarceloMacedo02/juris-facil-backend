package com.jurisfacil.processes.repository;

import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessPartyRepository extends JpaRepository<ProcessPartyEntity, UUID> {

    List<ProcessPartyEntity> findByOrganizationIdAndProcessId(UUID organizationId, UUID processId);
}
