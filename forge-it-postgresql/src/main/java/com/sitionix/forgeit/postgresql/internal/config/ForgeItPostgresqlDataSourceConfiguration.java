package com.sitionix.forgeit.postgresql.internal.config;

import com.sitionix.forgeit.postgresql.api.PostgresqlSupport;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({PostgresqlSupport.class, HikariDataSource.class})
@ConditionalOnProperty(prefix = PostgresqlProperties.PROPERTY_PREFIX, name = "enabled", havingValue = "true")
public class ForgeItPostgresqlDataSourceConfiguration {

    private static final String JDBC_URL_PROPERTY = "forge-it.postgresql.connection.jdbc-url";
    private static final String USERNAME_PROPERTY = "forge-it.postgresql.connection.username";
    private static final String PASSWORD_PROPERTY = "forge-it.postgresql.connection.password";

    @Bean(name = "forgeItPostgresDataSource")
    @Primary
    @DependsOn("postgresqlContainerManager")
    DataSource forgeItPostgresDataSource(final Environment environment) {
        final String jdbcUrl = environment.getProperty(JDBC_URL_PROPERTY);
        if (!StringUtils.hasText(jdbcUrl)) {
            throw new IllegalStateException("Forge-IT PostgreSQL is enabled but jdbc-url was not published. "
                    + "Expected property: forge-it.postgresql.connection.jdbc-url");
        }

        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(environment.getProperty(USERNAME_PROPERTY));
        dataSource.setPassword(environment.getProperty(PASSWORD_PROPERTY));
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }
}
