package com.sitionix.forgeit.postgresql.internal.config;

import com.sitionix.forgeit.domain.model.sql.RelationalModuleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlContainerManagerTests {

    @Test
    void shouldPublishConnectionPropertiesInExternalMode() {
        final MockEnvironment environment = new MockEnvironment();
        final PostgresqlProperties properties = new PostgresqlProperties();
        properties.setEnabled(true);
        properties.setMode(RelationalModuleProperties.Mode.EXTERNAL);

        final PostgresqlProperties.Connection connection = new PostgresqlProperties.Connection();
        connection.setHost("db-host");
        connection.setPort(6432);
        connection.setDatabase("forge-it-db");
        connection.setUsername("forge-user");
        connection.setPassword("forge-pass");
        properties.setConnection(connection);

        final PostgresqlContainerManager manager = new PostgresqlContainerManager(environment, properties);

        manager.afterPropertiesSet();

        assertThat(environment.getProperty("forge-it.postgresql.connection.jdbc-url"))
                .isEqualTo("jdbc:postgresql://db-host:6432/forge-it-db");
        assertThat(environment.getProperty("forge-it.postgresql.connection.host")).isEqualTo("db-host");
        assertThat(environment.getProperty("forge-it.postgresql.connection.port")).isEqualTo("6432");
        assertThat(environment.getProperty("forge-it.postgresql.connection.database")).isEqualTo("forge-it-db");
        assertThat(environment.getProperty("forge-it.postgresql.connection.username")).isEqualTo("forge-user");
        assertThat(environment.getProperty("forge-it.postgresql.connection.password")).isEqualTo("forge-pass");
    }

    @Test
    void shouldRejectExternalModeWithoutHost() {
        final MockEnvironment environment = new MockEnvironment();
        final PostgresqlProperties properties = new PostgresqlProperties();
        properties.setEnabled(true);
        properties.setMode(RelationalModuleProperties.Mode.EXTERNAL);

        final PostgresqlProperties.Connection connection = new PostgresqlProperties.Connection();
        connection.setPort(5432);
        properties.setConnection(connection);

        final PostgresqlContainerManager manager = new PostgresqlContainerManager(environment, properties);

        assertThatThrownBy(manager::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forge-it.modules.postgresql.connection.host");
    }

    @Test
    void shouldRejectExternalModeWithoutPort() {
        final MockEnvironment environment = new MockEnvironment();
        final PostgresqlProperties properties = new PostgresqlProperties();
        properties.setEnabled(true);
        properties.setMode(RelationalModuleProperties.Mode.EXTERNAL);

        final PostgresqlProperties.Connection connection = new PostgresqlProperties.Connection();
        connection.setHost("db-host");
        properties.setConnection(connection);

        final PostgresqlContainerManager manager = new PostgresqlContainerManager(environment, properties);

        assertThatThrownBy(manager::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forge-it.modules.postgresql.connection.port");
    }

}
