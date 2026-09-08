package com.jurisfacil.processes.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jurisfacil.support.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class V6ProcessesMigrationIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsProcessAndPartyTablesWithIndexes() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN ('processes', 'process_parties')",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND indexname IN ('uq_processes_cnj_tenant_active', "
                        + "'idx_processes_org_status_updated', 'idx_processes_org_title_trgm', "
                        + "'uq_process_parties_one_client', 'idx_process_parties_org_process')",
                Integer.class)).isEqualTo(5);
    }

    @Test
    void enforcesOneActiveCnjAndOneClientPartyPerProcess() {
        UUID organizationId = insertOrganization("migration-" + UUID.randomUUID() + "@example.com");
        UUID processId = insertProcess(organizationId, "00000000000000000000");

        assertThatThrownBy(() -> insertProcess(organizationId, "00000000000000000000"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update("INSERT INTO process_parties (organization_id, process_id, name, role, is_client) "
                + "VALUES (?, ?, ?, 'AUTOR', true)", organizationId, processId, "Client One");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO process_parties (organization_id, process_id, name, role, is_client) "
                        + "VALUES (?, ?, ?, 'REU', true)", organizationId, processId, "Client Two"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertOrganization(String email) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email) VALUES (?, ?) RETURNING organization_id",
                UUID.class, "Migration Office", email);
    }

    private UUID insertProcess(UUID organizationId, String cnj) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO processes (organization_id, cnj_number, title, court, court_unit, location) "
                        + "VALUES (?, ?, 'Process', 'TJCE', 'Unit', 'Fortaleza') RETURNING process_id",
                UUID.class, organizationId, cnj);
    }
}
