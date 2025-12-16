package com.sitionix.forgeit.postgresql.internal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ForgeItPostgresqlDataSourceConfigurationTests {

    private final ForgeItPostgresqlDataSourceConfiguration configuration =
            new ForgeItPostgresqlDataSourceConfiguration();

    @Test
    void shouldFailFastWhenJdbcUrlMissing() {
        final MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> this.configuration.forgeItPostgresDataSource(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forge-it.postgresql.connection.jdbc-url");
    }

    @Test
    void shouldBuildDataSourceFromForgeItPropertiesOnly() {
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("forge-it.postgresql.connection.jdbc-url", "jdbc:postgresql://localhost:5432/it-db")
                .withProperty("forge-it.postgresql.connection.username", "it-user")
                .withProperty("forge-it.postgresql.connection.password", "it-pass")
                .withProperty("spring.datasource.url", "jdbc:postgresql://ignored:5432/ignored-db");

        final HikariDataSource dataSource =
                (HikariDataSource) this.configuration.forgeItPostgresDataSource(environment);
        try {
            assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/it-db");
            assertThat(dataSource.getUsername()).isEqualTo("it-user");
            assertThat(dataSource.getPassword()).isEqualTo("it-pass");
        } finally {
            dataSource.close();
        }
    }
}
