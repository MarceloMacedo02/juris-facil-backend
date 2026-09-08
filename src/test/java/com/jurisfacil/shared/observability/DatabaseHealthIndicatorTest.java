package com.jurisfacil.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class DatabaseHealthIndicatorTest {

    @Test
    void reportsUpForValidConnection() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);

        assertThat(new DatabaseHealthIndicator(dataSource).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWhenConnectionFails() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("database unavailable"));

        assertThat(new DatabaseHealthIndicator(dataSource).health().getStatus()).isEqualTo(Status.DOWN);
    }
}
