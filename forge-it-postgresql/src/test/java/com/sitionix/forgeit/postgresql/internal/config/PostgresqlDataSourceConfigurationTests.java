package com.sitionix.forgeit.postgresql.internal.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlDataSourceConfigurationTests {

    private final PostgresqlDataSourceConfiguration configuration = new PostgresqlDataSourceConfiguration();

    @Test
    void resolvesConnectionDetailsFromForgeItPropertiesIgnoringSpringDatasource() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://spring-host:1111/spring-db");
        environment.setProperty("spring.datasource.username", "spring-user");
        environment.setProperty("spring.datasource.password", "spring-pass");
        environment.setProperty("forge-it.postgresql.connection.jdbc-url", "jdbc:postgresql://forge-host:6432/forge-db");
        environment.setProperty("forge-it.postgresql.connection.username", "forge-user");
        environment.setProperty("forge-it.postgresql.connection.password", "forge-pass");

        final PostgresqlProperties properties = new PostgresqlProperties();
        final PostgresqlProperties.Connection connection = new PostgresqlProperties.Connection();
        connection.setDatabase("should-not-be-used");
        connection.setUsername("default-user");
        connection.setPassword("default-pass");
        properties.setConnection(connection);

        final PostgresqlDataSourceConfiguration.PostgresqlConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        assertThat(details.jdbcUrl()).isEqualTo("jdbc:postgresql://forge-host:6432/forge-db");
        assertThat(details.username()).isEqualTo("forge-user");
        assertThat(details.password()).isEqualTo("forge-pass");
    }

    @Test
    void resolvesConnectionDetailsFromModuleDefaultsWhenForgeItOverridesAbsent() {
        final MockEnvironment environment = new MockEnvironment();

        final PostgresqlProperties properties = new PostgresqlProperties();
        final PostgresqlProperties.Connection connection = new PostgresqlProperties.Connection();
        connection.setDatabase("forge-it-db");
        connection.setUsername("forge-it-user");
        connection.setPassword("forge-it-pass");
        connection.setHost("module-host");
        connection.setPort(6543);
        properties.setConnection(connection);

        final PostgresqlDataSourceConfiguration.PostgresqlConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        assertThat(details.jdbcUrl()).isEqualTo("jdbc:postgresql://module-host:6543/forge-it-db");
        assertThat(details.username()).isEqualTo("forge-it-user");
        assertThat(details.password()).isEqualTo("forge-it-pass");
    }
}
