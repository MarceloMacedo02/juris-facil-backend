package com.jurisfacil.iam.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jurisfacil.support.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class V1UsersMigrationIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsUsersSchemaWithRequiredColumnsAndIndexes() {
        Map<String, Integer> columns = jdbcTemplate.query(
                "SELECT column_name, ordinal_position FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'users'",
                rs -> {
                    Map<String, Integer> result = new java.util.HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString("column_name"), rs.getInt("ordinal_position"));
                    }
                    return result;
                });

        assertThat(columns).containsKeys(
                "user_id", "email", "name", "password_hash", "password_set_at",
                "reset_token_hash", "reset_expires_at", "platform_role", "status",
                "created_at", "updated_at");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND tablename = 'users' AND indexname IN "
                        + "('uq_users_email_lower', 'idx_users_email', 'idx_users_status', "
                        + "'idx_users_platform_role')",
                Integer.class)).isEqualTo(4);
    }

    @Test
    void enforcesCaseInsensitiveEmailAndAllowedUserValues() {
        jdbcTemplate.update("INSERT INTO users (email, name) VALUES (?, ?)",
                "migration.user@example.com", "Migration User");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (email, name) VALUES (?, ?)",
                "MIGRATION.USER@EXAMPLE.COM", "Duplicate User"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (email, name, status) VALUES (?, ?, ?)",
                "invalid.status@example.com", "Invalid Status", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (email, name, platform_role) VALUES (?, ?, ?)",
                "invalid.role@example.com", "Invalid Role", "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
