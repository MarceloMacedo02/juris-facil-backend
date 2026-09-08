package com.jurisfacil.iam.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jurisfacil.support.BaseIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class V1RefreshMigrationIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsRefreshSessionSchemaAndDefaults() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'refresh_sessions'",
                Integer.class)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND tablename = 'refresh_sessions' AND indexname IN "
                        + "('uq_refresh_sessions_token_hash', 'idx_refresh_sessions_user', "
                        + "'idx_refresh_sessions_expires')",
                Integer.class)).isEqualTo(3);
    }

    @Test
    void enforcesUserReferenceAndUniqueTokenHash() {
        UUID userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, name) VALUES (?, ?) RETURNING user_id",
                UUID.class, "refresh.user@example.com", "Refresh User");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        UUID sessionId = jdbcTemplate.queryForObject(
                "INSERT INTO refresh_sessions (user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?) RETURNING session_id",
                UUID.class, userId, "a".repeat(64), expiresAt);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT platform_session FROM refresh_sessions WHERE session_id = ?",
                Boolean.class, sessionId)).isFalse();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO refresh_sessions (user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?)", userId, "a".repeat(64), expiresAt))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO refresh_sessions (user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?)", UUID.randomUUID(), "b".repeat(64), expiresAt))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void supportsRotationChainAndCascadesWithUser() {
        UUID userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, name) VALUES (?, ?) RETURNING user_id",
                UUID.class, "rotation.user@example.com", "Rotation User");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
        UUID firstSession = jdbcTemplate.queryForObject(
                "INSERT INTO refresh_sessions (user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?) RETURNING session_id",
                UUID.class, userId, "c".repeat(64), expiresAt);
        UUID secondSession = jdbcTemplate.queryForObject(
                "INSERT INTO refresh_sessions (user_id, token_hash, expires_at, replaced_by, revoked_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) RETURNING session_id",
                UUID.class, userId, "d".repeat(64), expiresAt, firstSession);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT replaced_by FROM refresh_sessions WHERE session_id = ?",
                UUID.class, secondSession)).isEqualTo(firstSession);

        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_sessions WHERE user_id = ?",
                Integer.class, userId)).isZero();
    }
}
