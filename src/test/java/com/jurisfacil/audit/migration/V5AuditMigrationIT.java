package com.jurisfacil.audit.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jurisfacil.support.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class V5AuditMigrationIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAppendOnlyAuditTableAndIndexes() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'audit_events'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND indexname IN ('idx_audit_org_created', 'idx_audit_actor_created')",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void allowsAdminEventsWithoutOrganizationAndRejectsUnknownActionsAndOversizedPayloads() {
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO audit_events "
                        + "(audit_event_id, action, resource_type, payload) VALUES (?, 'LOGIN_SUCCESS', ?, ?::jsonb)",
                eventId, "AUTH", "{\"source\":\"admin\"}");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT organization_id FROM audit_events WHERE audit_event_id = ?",
                UUID.class, eventId)).isNull();

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO audit_events "
                        + "(action, resource_type) VALUES ('NOT_CANONICAL', 'AUTH')"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO audit_events "
                        + "(action, resource_type, payload) VALUES ('LOGIN_SUCCESS', 'AUTH', ?::jsonb)",
                        "{\"data\":\"" + "x".repeat(4100) + "\"}"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
