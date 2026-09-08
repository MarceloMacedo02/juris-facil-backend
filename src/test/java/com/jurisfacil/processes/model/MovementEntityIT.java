package com.jurisfacil.processes.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.processes.model.entity.ProcessMovementEntity;
import com.jurisfacil.processes.model.enums.MovementSource;
import com.jurisfacil.processes.model.enums.MovementType;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
class MovementEntityIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @Transactional
    void persistsMovementWithManualSourceAndOptimisticVersion() {
        UUID organizationId = insertOrganization();
        UUID processId = insertProcess(organizationId);
        ProcessMovementEntity movement = ProcessMovementEntity.builder()
                .organizationId(organizationId)
                .processId(processId)
                .movementDate(OffsetDateTime.parse("2026-01-10T10:00:00Z"))
                .movementType(MovementType.DECISAO)
                .title("Decisão publicada")
                .description("Descrição")
                .build();

        entityManager.persist(movement);
        entityManager.flush();

        assertThat(movement.getId()).isNotNull();
        assertThat(movement.getSource()).isEqualTo(MovementSource.MANUAL);
        assertThat(movement.getVersion()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT movement_type FROM process_movements WHERE movement_id = ?",
                String.class, movement.getId())).isEqualTo("DECISAO");
    }

    private UUID insertOrganization() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email) VALUES (?, ?) RETURNING organization_id",
                UUID.class, "Movement Office", "movement-" + UUID.randomUUID() + "@example.com");
    }

    private UUID insertProcess(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO processes (organization_id, title, court, court_unit, location) "
                        + "VALUES (?, 'Process', 'TJCE', 'Unit', 'Fortaleza') RETURNING process_id",
                UUID.class, organizationId);
    }
}
