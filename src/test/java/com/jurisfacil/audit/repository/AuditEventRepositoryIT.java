package com.jurisfacil.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.model.AuditEventEntity;
import com.jurisfacil.support.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditEventRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private AuditEventRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndReadsEventsByOrganizationInReverseChronologicalOrder() {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO organizations "
                        + "(organization_id, name, contact_email) VALUES (?, ?, ?)",
                organizationId, "Audit Organization", organizationId + "@example.com");
        repository.save(new AuditEventEntity(event(organizationId, AuditAction.PROCESS_CREATED), "{}", null, null));
        repository.save(new AuditEventEntity(event(organizationId, AuditAction.PROCESS_UPDATED), "{}", null, null));

        assertThat(repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId))
                .hasSize(2)
                .extracting(AuditEventEntity::getAction)
                .containsExactly(AuditAction.PROCESS_UPDATED, AuditAction.PROCESS_CREATED);
    }

    private AuditEvent event(UUID organizationId, AuditAction action) {
        return AuditEvent.builder().action(action).organizationId(organizationId).resourceType("PROCESS").build();
    }
}
