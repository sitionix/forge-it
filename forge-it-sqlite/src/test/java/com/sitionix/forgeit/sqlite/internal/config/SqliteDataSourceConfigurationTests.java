package com.sitionix.forgeit.sqlite.internal.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqliteDataSourceConfigurationTests {

    private final SqliteDataSourceConfiguration configuration = new SqliteDataSourceConfiguration();

    @Test
    void resolvesJdbcUrlFromEnvironment() {
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("forge-it.sqlite.connection.jdbc-url", "jdbc:sqlite:/tmp/forge-it.db");
        final SqliteProperties properties = new SqliteProperties();
        properties.setConnection(new SqliteProperties.Connection());

        final SqliteDataSourceConfiguration.SqliteConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        assertThat(details.jdbcUrl()).isEqualTo("jdbc:sqlite:/tmp/forge-it.db");
        assertThat(details.username()).isEmpty();
        assertThat(details.password()).isEmpty();
    }

    @Test
    void resolvesJdbcUrlFromDatabasePath() {
        final MockEnvironment environment = new MockEnvironment();
        final SqliteProperties properties = new SqliteProperties();
        final SqliteProperties.Connection connection = new SqliteProperties.Connection();
        connection.setDatabase("/tmp/custom.db");
        properties.setConnection(connection);

        final SqliteDataSourceConfiguration.SqliteConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        assertThat(details.jdbcUrl()).isEqualTo("jdbc:sqlite:/tmp/custom.db");
    }

    @Test
    void failsWhenConnectionIsMissing() {
        final MockEnvironment environment = new MockEnvironment();
        final SqliteProperties properties = new SqliteProperties();

        assertThatThrownBy(() -> this.configuration.resolveConnectionDetails(environment, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("forge-it.modules.sqlite.connection must be configured");
    }
}
