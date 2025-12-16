package com.sitionix.forgeit.postgresql.internal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
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
    @DependsOn("postgresqlContainerManager")
    DataSourceProperties postgresDataSourceProperties(final Environment environment,
                                                      final PostgresqlProperties postgresqlProperties) {
        final DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(this.resolveJdbcUrl(environment, postgresqlProperties));
        properties.setUsername(this.resolveWithDefault(environment, USERNAME_PROPERTY,
                postgresqlProperties.getConnection().getUsername()));
        properties.setPassword(this.resolveWithDefault(environment, PASSWORD_PROPERTY,
                postgresqlProperties.getConnection().getPassword()));
        properties.setDriverClassName("org.postgresql.Driver");
        return properties;
    }

    @Bean
    @Primary
    @DependsOn("postgresqlContainerManager")
    DataSource dataSource(final DataSourceProperties postgresDataSourceProperties) {
        return this.postgresDataSource(postgresDataSourceProperties);
    }

    @Bean(name = "postgresDataSource")
    @DependsOn("postgresqlContainerManager")
    DataSource postgresDataSource(final DataSourceProperties postgresDataSourceProperties) {
        return postgresDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    private String resolveJdbcUrl(final Environment environment, final PostgresqlProperties postgresqlProperties) {
        final String configuredUrl = environment.getProperty(JDBC_URL_PROPERTY);
        if (StringUtils.hasText(configuredUrl)) {
            return configuredUrl;
        }
        final String host = Objects.requireNonNullElse(postgresqlProperties.getConnection().getHost(), "localhost");
        final Integer port = Objects.requireNonNullElse(postgresqlProperties.getConnection().getPort(), 5432);
        final String database = Objects.requireNonNullElse(postgresqlProperties.getConnection().getDatabase(), "forge-it");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private String resolveWithDefault(final Environment environment, final String key, final String defaultValue) {
        return Objects.requireNonNullElse(environment.getProperty(key), defaultValue);
    }
}
