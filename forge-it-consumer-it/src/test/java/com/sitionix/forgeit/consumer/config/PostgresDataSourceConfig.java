package com.sitionix.forgeit.consumer.config;

import com.sitionix.forgeit.postgresql.internal.config.PostgresqlProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
public class PostgresDataSourceConfig {

    private static final String POSTGRES_PROPERTIES_PREFIX = "forge-it.postgresql";
    private static final String JDBC_URL_PROPERTY = POSTGRES_PROPERTIES_PREFIX + ".jdbc-url";
    private static final String USERNAME_PROPERTY = POSTGRES_PROPERTIES_PREFIX + ".username";
    private static final String PASSWORD_PROPERTY = POSTGRES_PROPERTIES_PREFIX + ".password";

    @Bean
    @DependsOn("postgresqlContainerManager")
    public DataSourceProperties postgresDataSourceProperties(final Environment environment,
                                                            final PostgresqlProperties postgresqlProperties) {
        final DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(resolveJdbcUrl(environment, postgresqlProperties));
        properties.setUsername(resolveWithDefault(environment, USERNAME_PROPERTY, postgresqlProperties.getUsername()));
        properties.setPassword(resolveWithDefault(environment, PASSWORD_PROPERTY, postgresqlProperties.getPassword()));
        properties.setDriverClassName("org.postgresql.Driver");
        return properties;
    }

    @Bean
    @Primary
    @DependsOn("postgresqlContainerManager")
    public DataSource dataSource(final DataSourceProperties postgresDataSourceProperties) {
        return postgresDataSource(postgresDataSourceProperties);
    }

    @Bean(name = "postgresDataSource")
    @DependsOn("postgresqlContainerManager")
    public DataSource postgresDataSource(final DataSourceProperties postgresDataSourceProperties) {
        return postgresDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    private String resolveJdbcUrl(final Environment environment, final PostgresqlProperties postgresqlProperties) {
        final String configuredUrl = environment.getProperty(JDBC_URL_PROPERTY);
        if (StringUtils.hasText(configuredUrl)) {
            return configuredUrl;
        }
        final String host = Objects.requireNonNullElse(postgresqlProperties.getHost(), "localhost");
        final Integer port = Objects.requireNonNullElse(postgresqlProperties.getPort(), 5432);
        final String database = Objects.requireNonNullElse(postgresqlProperties.getDatabase(), "forge-it");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private String resolveWithDefault(final Environment environment, final String key, final String defaultValue) {
        return Objects.requireNonNullElse(environment.getProperty(key), defaultValue);
    }
}
