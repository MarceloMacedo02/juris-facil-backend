package com.jurisfacil.processes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jurisfacil.processes.mapper.MovementMapper;
import com.jurisfacil.shared.tenant.TenantContext;
import com.jurisfacil.shared.tenant.TenantContextHolder;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MovementListIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovementListService movementListService;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void listsTenantMovementsDescendingAndResolvesAuthorName() {
        UUID organizationId = insertOrganization("list");
        UUID processId = insertProcess(organizationId);
        UUID userId = insertUser();
        UUID membershipId = insertMembership(organizationId, userId);
        insertMovement(organizationId, processId, membershipId, "2026-02-01T10:00:00Z", "Older");
        insertMovement(organizationId, processId, membershipId, "2026-02-02T10:00:00Z", "Newer");
        TenantContextHolder.set(new TenantContext(organizationId, "LAWYER", List.of("PROCESS")));

        var page = movementListService.list(organizationId, processId, 0, 50);

        assertThat(page.items()).extracting(item -> item.title()).containsExactly("Newer", "Older");
        assertThat(page.items()).allSatisfy(item -> assertThat(item.authorName()).isEqualTo("Timeline User"));
        assertThat(page.totalItems()).isEqualTo(2);
    }

    @Test
    void hidesCrossTenantProcessAsNotFound() {
        UUID organizationId = insertOrganization("cross-tenant");
        UUID processId = insertProcess(organizationId);
        TenantContextHolder.set(new TenantContext(UUID.randomUUID(), "LAWYER", List.of("PROCESS")));

        assertThatThrownBy(() -> movementListService.list(UUID.randomUUID(), processId, 0, 50))
                .isInstanceOf(ProcessDetailService.ProcessNotFoundException.class);
    }

    private UUID insertOrganization(String suffix) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email) VALUES (?, ?) RETURNING organization_id",
                UUID.class, "Movement List Office", suffix + "-" + UUID.randomUUID() + "@example.com");
    }

    private UUID insertProcess(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO processes (organization_id, title, court, court_unit, location) "
                        + "VALUES (?, 'Timeline Process', 'TJCE', 'Unit', 'Fortaleza') RETURNING process_id",
                UUID.class, organizationId);
    }

    private UUID insertUser() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (name, email, status) VALUES ('Timeline User', ?, 'ACTIVE') RETURNING user_id",
                UUID.class, "timeline-" + UUID.randomUUID() + "@example.com");
    }

    private UUID insertMembership(UUID organizationId, UUID userId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO memberships (organization_id, user_id, role, status) "
                        + "VALUES (?, ?, 'LAWYER', 'ACTIVE') RETURNING membership_id",
                UUID.class, organizationId, userId);
    }

    private void insertMovement(UUID organizationId, UUID processId, UUID membershipId,
            String date, String title) {
        jdbcTemplate.update(
                "INSERT INTO process_movements (organization_id, process_id, movement_date, movement_type, "
                        + "title, source, created_by) VALUES (?, ?, ?, 'OUTRO', ?, 'MANUAL', ?)",
                organizationId, processId, OffsetDateTime.parse(date), title, membershipId);
    }
}
