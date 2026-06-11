package com.sitionix.forgeit.sqlite.internal.config;

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
public class SqliteDataSourceConfiguration {

    private static final String SQLITE_PROPERTIES_PREFIX = "forge-it.sqlite.connection";
    private static final String JDBC_URL_PROPERTY = SQLITE_PROPERTIES_PREFIX + ".jdbc-url";
    private static final String USERNAME_PROPERTY = SQLITE_PROPERTIES_PREFIX + ".username";
    private static final String PASSWORD_PROPERTY = SQLITE_PROPERTIES_PREFIX + ".password";

    @Bean
    @Primary
    @DependsOn("sqliteDatabaseManager")
    DataSource dataSource(@Qualifier("sqliteDataSource") final DataSource sqliteDataSource) {
        return sqliteDataSource;
    }

    @Bean(name = "sqliteDataSource")
    @DependsOn("sqliteDatabaseManager")
    DataSource sqliteDataSource(final Environment environment,
                                final SqliteProperties sqliteProperties) {
        final SqliteConnectionDetails connectionDetails = this.resolveConnectionDetails(environment, sqliteProperties);
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(connectionDetails.jdbcUrl());
        dataSource.setUsername(connectionDetails.username());
        dataSource.setPassword(connectionDetails.password());
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setMaximumPoolSize(1);
        return dataSource;
    }

    SqliteConnectionDetails resolveConnectionDetails(final Environment environment,
                                                     final SqliteProperties sqliteProperties) {
        final SqliteProperties.Connection connection = Objects.requireNonNull(sqliteProperties.getConnection(),
                "forge-it.modules.sqlite.connection must be configured");
        final String jdbcUrl = this.requireText(this.resolveJdbcUrl(environment, sqliteProperties),
                "JDBC URL", JDBC_URL_PROPERTY);
        final String username = this.resolveWithDefault(environment, USERNAME_PROPERTY,
                Objects.requireNonNullElse(connection.getUsername(), ""));
        final String password = this.resolveWithDefault(environment, PASSWORD_PROPERTY,
                Objects.requireNonNullElse(connection.getPassword(), ""));
        return new SqliteConnectionDetails(jdbcUrl, username, password);
    }

    private String resolveJdbcUrl(final Environment environment, final SqliteProperties sqliteProperties) {
        final String configuredUrl = environment.getProperty(JDBC_URL_PROPERTY);
        if (StringUtils.hasText(configuredUrl)) {
            return configuredUrl;
        }
        final SqliteProperties.Connection connection = Objects.requireNonNull(sqliteProperties.getConnection(),
                "forge-it.modules.sqlite.connection must be configured");
        final String database = Objects.requireNonNullElse(connection.getDatabase(), ":memory:");
        return "jdbc:sqlite:" + database;
    }

    private String resolveWithDefault(final Environment environment, final String key, final String defaultValue) {
        return Objects.requireNonNullElse(environment.getProperty(key), defaultValue);
    }

    private String requireText(final String value, final String label, final String propertyKey) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        throw new IllegalStateException("SQLite " + label + " must be configured via " + propertyKey
                + " or forge-it.modules.sqlite.connection");
    }

    record SqliteConnectionDetails(String jdbcUrl, String username, String password) {
    }
}
