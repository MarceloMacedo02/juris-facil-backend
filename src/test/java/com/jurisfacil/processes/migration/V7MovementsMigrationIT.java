package com.jurisfacil.processes.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.support.BaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class V7MovementsMigrationIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsMovementsTableAndRequiredIndexes() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name = 'process_movements'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND indexname IN ('idx_movements_process_cron', 'idx_movements_org_process')",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void exposesChronologicalIndexToThePlanner() {
        jdbcTemplate.execute("SET enable_seqscan = off");
        try {
            List<String> explain = jdbcTemplate.queryForList(
                    "EXPLAIN SELECT * FROM process_movements "
                            + "WHERE process_id = '00000000-0000-0000-0000-000000000001' "
                            + "ORDER BY movement_date DESC",
                    String.class);
            assertThat(String.join(" ", explain)).contains("idx_movements_process_cron");
        } finally {
            jdbcTemplate.execute("RESET enable_seqscan");
        }
    }
}
