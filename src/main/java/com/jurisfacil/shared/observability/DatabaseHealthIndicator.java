package com.jurisfacil.shared.observability;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
public final class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            if (connection.isValid(1)) {
                return Health.up().build();
            }
            return Health.down().withDetail("database", "connection invalid").build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
