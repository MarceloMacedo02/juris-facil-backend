package com.jurisfacil.processes.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.processes.model.entity.ProcessMovementEntity;
import com.jurisfacil.processes.model.enums.MovementType;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

class MovementRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovementRepository movementRepository;

    @Test
    void returnsMovementsInReverseChronologicalOrderAndFindsLatest() {
        UUID organizationId = insertOrganization();
        UUID processId = insertProcess(organizationId);
        ProcessMovementEntity older = movement(organizationId, processId, "2026-01-01T10:00:00Z", "Older");
        ProcessMovementEntity newer = movement(organizationId, processId, "2026-01-02T10:00:00Z", "Newer");
        movementRepository.saveAll(java.util.List.of(older, newer));

        assertThat(movementRepository.findFirstByProcessIdAndOrganizationIdOrderByMovementDateDesc(
                processId, organizationId))
                .get()
                .extracting(ProcessMovementEntity::getId)
                .isEqualTo(newer.getId());
        assertThat(movementRepository.findByOrganizationIdAndProcessIdOrderByMovementDateDesc(
                organizationId, processId, PageRequest.of(0, 10)).getContent())
                .extracting(ProcessMovementEntity::getTitle)
                .containsExactly("Newer", "Older");
    }

    private ProcessMovementEntity movement(UUID organizationId, UUID processId, String date, String title) {
        return ProcessMovementEntity.builder()
                .organizationId(organizationId)
                .processId(processId)
                .movementDate(OffsetDateTime.parse(date))
                .movementType(MovementType.OUTRO)
                .title(title)
                .build();
    }

    private UUID insertOrganization() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email) VALUES (?, ?) RETURNING organization_id",
                UUID.class, "Repository Office", "repository-" + UUID.randomUUID() + "@example.com");
    }

    private UUID insertProcess(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO processes (organization_id, title, court, court_unit, location) "
                        + "VALUES (?, 'Process', 'TJCE', 'Unit', 'Fortaleza') RETURNING process_id",
                UUID.class, organizationId);
    }
}
