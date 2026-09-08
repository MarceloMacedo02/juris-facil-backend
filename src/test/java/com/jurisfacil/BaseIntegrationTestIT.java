package com.jurisfacil;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class BaseIntegrationTestIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startsPostgresAndAppliesFlywayBaseline() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '0' AND success = true",
                Integer.class);

        assertThat(appliedMigrations).isEqualTo(1);
    }
}
