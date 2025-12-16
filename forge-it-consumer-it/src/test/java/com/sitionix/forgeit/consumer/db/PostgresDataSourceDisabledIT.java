package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestPropertySource(properties = {
        "forge-it.modules.postgresql.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:consumer-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostgresDataSourceDisabledIT {

    @SuppressWarnings("unused")
    @Autowired
    private ForgeItSupport forgeIt;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void shouldLeaveUserDataSourceWhenPostgresqlModuleDisabled() {
        assertThat(this.applicationContext.containsBean("forgeItPostgresDataSource")).isFalse();
        assertThat(this.applicationContext.containsBean("postgresqlContainerManager")).isFalse();

        assertThat(this.dataSource).isInstanceOf(HikariDataSource.class);
        final HikariDataSource hikari = (HikariDataSource) this.dataSource;

        assertThat(hikari.getJdbcUrl())
                .isEqualTo("jdbc:h2:mem:consumer-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        assertThat(this.environment.getProperty("forge-it.postgresql.connection.jdbc-url")).isNull();
    }
}
