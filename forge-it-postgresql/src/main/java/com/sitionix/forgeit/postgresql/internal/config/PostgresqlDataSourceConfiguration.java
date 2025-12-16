package com.sitionix.forgeit.postgresql.internal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
public class PostgresqlDataSourceConfiguration {

    private static final String POSTGRES_PROPERTIES_PREFIX = "forge-it.postgresql.connection";
    private static final String JDBC_URL_PROPERTY = POSTGRES_PROPERTIES_PREFIX + ".jdbc-url";
    private static final String USERNAME_PROPERTY = POSTGRES_PROPERTIES_PREFIX + ".username";
    private static final String PASSWORD_PROPERTY = POSTGRES_PROPERTIES_PREFIX + ".password";

    @Bean
    @Primary
    @DependsOn("postgresqlContainerManager")
    DataSource dataSource(@Qualifier("postgresDataSource") final DataSource postgresDataSource) {
        return postgresDataSource;
    }

    @Bean(name = "postgresDataSource")
    @DependsOn("postgresqlContainerManager")
    DataSource postgresDataSource(final Environment environment,
                                  final PostgresqlProperties postgresqlProperties) {
        final PostgresqlConnectionDetails connectionDetails = this.resolveConnectionDetails(environment, postgresqlProperties);
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(connectionDetails.jdbcUrl());
        dataSource.setUsername(connectionDetails.username());
        dataSource.setPassword(connectionDetails.password());
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    PostgresqlConnectionDetails resolveConnectionDetails(final Environment environment,
                                                         final PostgresqlProperties postgresqlProperties) {
        final PostgresqlProperties.Connection connection = Objects.requireNonNull(postgresqlProperties.getConnection(),
                "forge-it.modules.postgresql.connection must be configured");
        final String jdbcUrl = this.requireText(this.resolveJdbcUrl(environment, postgresqlProperties),
                "JDBC URL", JDBC_URL_PROPERTY);
        final String username = this.requireText(this.resolveWithDefault(environment, USERNAME_PROPERTY,
                connection.getUsername()), "username", USERNAME_PROPERTY);
        final String password = this.requireText(this.resolveWithDefault(environment, PASSWORD_PROPERTY,
                connection.getPassword()), "password", PASSWORD_PROPERTY);
        return new PostgresqlConnectionDetails(jdbcUrl, username, password);
    }

    private String resolveJdbcUrl(final Environment environment, final PostgresqlProperties postgresqlProperties) {
        final String configuredUrl = environment.getProperty(JDBC_URL_PROPERTY);
        if (StringUtils.hasText(configuredUrl)) {
            return configuredUrl;
        }
        final PostgresqlProperties.Connection connection = Objects.requireNonNull(postgresqlProperties.getConnection(),
                "forge-it.modules.postgresql.connection must be configured");
        final String host = Objects.requireNonNullElse(connection.getHost(), "localhost");
        final Integer port = Objects.requireNonNullElse(connection.getPort(), 5432);
        final String database = Objects.requireNonNullElse(connection.getDatabase(), "forge-it");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private String resolveWithDefault(final Environment environment, final String key, final String defaultValue) {
        return Objects.requireNonNullElse(environment.getProperty(key), defaultValue);
    }

    private String requireText(final String value, final String label, final String propertyKey) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        throw new IllegalStateException("PostgreSQL " + label + " must be configured via " + propertyKey
                + " or forge-it.modules.postgresql.connection");
    }

    record PostgresqlConnectionDetails(String jdbcUrl, String username, String password) {
    }
}
