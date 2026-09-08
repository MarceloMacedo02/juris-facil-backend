package com.jurisfacil.organizations.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jurisfacil.support.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class V2OrganizationsMigrationIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsOrganizationBoundedContextSchemaAndModuleCatalog() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN ('organizations', 'subscriptions', 'modules', "
                        + "'module_entitlements', 'memberships')",
                Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM modules", Integer.class)).isEqualTo(9);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND indexname IN ('uq_organizations_cnpj_cpf', "
                        + "'uq_subscriptions_organization', 'uq_entitlements_org_module', "
                        + "'uq_memberships_org_user_active', 'uq_memberships_user_active')",
                Integer.class)).isEqualTo(5);
    }

    @Test
    void enforcesOrganizationCnpjAndSingleActiveWorkspaceMembership() {
        String firstEmail = "org-migration-" + UUID.randomUUID() + "@example.com";
        String secondEmail = "org-migration-" + UUID.randomUUID() + "@example.com";
        UUID firstOrg = insertOrganization("11.111.111/0001-11");
        UUID secondOrg = insertOrganization("22.222.222/0001-22");
        UUID user = insertUser(firstEmail);

        assertThatThrownBy(() -> insertOrganization("11.111.111/0001-11"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertMembership(firstOrg, user, "ACTIVE");
        assertThatThrownBy(() -> insertMembership(secondOrg, user, "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertUser(secondEmail);
    }

    private UUID insertOrganization(String cnpjCpf) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO organizations "
                        + "(organization_id, name, cnpj_cpf, contact_email) VALUES (?, ?, ?, ?)",
                id, "Migration Organization", cnpjCpf, "contact-" + id + "@example.com");
        return id;
    }

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id, email, name) VALUES (?, ?, ?)",
                id, email, "Migration User");
        return id;
    }

    private void insertMembership(UUID organizationId, UUID userId, String status) {
        jdbcTemplate.update("INSERT INTO memberships "
                        + "(organization_id, user_id, role, status) VALUES (?, ?, 'LAWYER', ?)",
                organizationId, userId, status);
    }
}
