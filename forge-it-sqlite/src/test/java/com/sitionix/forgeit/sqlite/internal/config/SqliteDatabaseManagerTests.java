package com.sitionix.forgeit.sqlite.internal.config;

import com.sitionix.forgeit.domain.model.sql.RelationalModuleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteDatabaseManagerTests {

    @Test
    void publishesExternalJdbcUrl() {
        final MockEnvironment environment = new MockEnvironment();
        final SqliteProperties properties = new SqliteProperties();
        final SqliteProperties.Connection connection = new SqliteProperties.Connection();
        connection.setJdbcUrl("jdbc:sqlite:/tmp/external.db");
        properties.setEnabled(true);
        properties.setMode(RelationalModuleProperties.Mode.EXTERNAL);
        properties.setConnection(connection);

        final SqliteDatabaseManager manager = new SqliteDatabaseManager(environment, properties);
        manager.afterPropertiesSet();

        assertThat(environment.getProperty("forge-it.sqlite.connection.jdbc-url"))
                .isEqualTo("jdbc:sqlite:/tmp/external.db");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.sqlite.JDBC");

        manager.destroy();
    }
}
