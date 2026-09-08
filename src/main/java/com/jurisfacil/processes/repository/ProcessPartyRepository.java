package com.jurisfacil.processes.repository;

import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import com.jurisfacil.processes.service.ProcessListService.ClientNameProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProcessPartyRepository extends JpaRepository<ProcessPartyEntity, UUID> {

    List<ProcessPartyEntity> findByOrganizationIdAndProcessId(UUID organizationId, UUID processId);

    List<ProcessPartyEntity> findByOrganizationIdAndProcessIdOrderByCreatedAtAsc(UUID organizationId, UUID processId);

    @Query("select p.processId as processId, p.name as name from ProcessPartyEntity p "
            + "where p.client = true and p.processId in :processIds")
    List<ClientNameProjection> findClientNamesByProcessIds(List<UUID> processIds);
}
