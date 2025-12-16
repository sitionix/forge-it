package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://example.invalid:6543/consumer-main-db",
        "spring.datasource.username=main-user",
        "spring.datasource.password=main-pass"
})
class PostgresDataSourceSelectionIT {

    @SuppressWarnings("unused")
    @Autowired
    private ForgeItSupport forgeIt;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void shouldUseForgeItDataSourceWhenModuleEnabledEvenIfSpringDatasourceIsPresent() {
        assertThat(this.environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://example.invalid:6543/consumer-main-db");

        assertThat(this.dataSource).isInstanceOf(HikariDataSource.class);

        final HikariDataSource hikari = (HikariDataSource) this.dataSource;
        final String forgeJdbcUrl = this.environment.getProperty("forge-it.postgresql.connection.jdbc-url");

        assertThat(forgeJdbcUrl).isNotBlank();
        assertThat(hikari.getJdbcUrl()).isEqualTo(forgeJdbcUrl);

        final String mappedPort = this.environment.getProperty("forge-it.postgresql.connection.port");
        assertThat(mappedPort).isNotBlank();
        assertThat(hikari.getJdbcUrl()).contains(":" + mappedPort + "/");

        assertThat(hikari.getJdbcUrl())
                .isNotEqualTo(this.environment.getProperty("spring.datasource.url"));
    }
}
