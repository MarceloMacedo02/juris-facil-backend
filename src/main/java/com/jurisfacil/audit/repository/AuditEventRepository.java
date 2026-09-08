package com.jurisfacil.audit.repository;

import com.jurisfacil.audit.model.AuditEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface AuditEventRepository extends Repository<AuditEventEntity, UUID> {

    AuditEventEntity save(AuditEventEntity entity);

    List<AuditEventEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
