package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.support.BaseIntegrationTest;

class EntitlementSeederIT extends BaseIntegrationTest {

    @Autowired EntitlementSeeder entitlementSeeder;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanEntitlements() {
        jdbcTemplate.update("DELETE FROM module_entitlements");
        jdbcTemplate.update("DELETE FROM organizations");
    }

    @Test
    void persistsProfessionalCatalogWithoutCoreRow() {
        UUID organizationId = jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name, contact_email, status) VALUES (?, ?, 'ACTIVE') RETURNING organization_id",
                UUID.class, "Seeder Test", "seeder@example.com");

        entitlementSeeder.seed(organizationId, PlanTier.PROFISSIONAL);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM module_entitlements WHERE organization_id = ?", Integer.class, organizationId))
                .isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM module_entitlements WHERE organization_id = ? AND enabled = true",
                Integer.class, organizationId)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM module_entitlements WHERE organization_id = ? AND module_code = 'CORE'",
                Integer.class, organizationId)).isZero();
    }
}
