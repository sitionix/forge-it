package com.sitionix.forgeit.postgresql.internal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public final class PostgresqlContainerManager implements InitializingBean, DisposableBean {

    private static final String PROPERTY_SOURCE_NAME = "forgeItPostgresql";
    private static final String DEFAULT_IMAGE = "postgres:16-alpine";

    private final ConfigurableEnvironment environment;
    private final PostgresqlProperties properties;

    private PostgreSQLContainer<?> container;
    private String jdbcUrl;
    private String host;
    private Integer port;

    @Override
    public void afterPropertiesSet() {
        validateEnabled();
        final PostgresqlProperties.Mode mode = requireMode();
        if (mode == PostgresqlProperties.Mode.EXTERNAL) {
            initialiseExternal();
        } else {
            startInternal();
        }
        publishEnvironment();
    }

    @Override
    public void destroy() {
        cleanupResources();
    }

    public String getJdbcUrl() {
        if (this.jdbcUrl == null) {
            throw new IllegalStateException("Postgresql JDBC URL has not been initialised");
        }
        return this.jdbcUrl;
    }

    public Integer getPort() {
        if (this.port == null) {
            throw new IllegalStateException("Postgresql port has not been initialised");
        }
        return this.port;
    }

    public String getHost() {
        if (this.host == null) {
            throw new IllegalStateException("Postgresql host has not been initialised");
        }
        return this.host;
    }

    private void cleanupResources() {
        removeEnvironment();
        if (this.container != null) {
            this.container.stop();
            this.container = null;
        }
        this.jdbcUrl = null;
        this.host = null;
        this.port = null;
    }

    private void publishEnvironment() {
        if (this.environment == null) {
            return;
        }
        if (this.jdbcUrl == null || this.host == null || this.port == null) {
            throw new IllegalStateException("Postgresql container not initialised");
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        final Map<String, Object> props = Map.of(
                "forge-it.postgresql.jdbc-url", this.jdbcUrl,
                "forge-it.postgresql.port", this.port,
                "forge-it.postgresql.host", this.host,
                "forge-it.postgresql.database", Objects.requireNonNullElse(this.properties.getDatabase(), "forgeit"),
                "forge-it.postgresql.username", Objects.requireNonNullElse(this.properties.getUsername(), "forgeit"),
                "forge-it.postgresql.password", Objects.requireNonNullElse(this.properties.getPassword(), "forgeit")
        );
        final MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, props);
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.replace(PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addFirst(propertySource);
        }
    }

    private void removeEnvironment() {
        if (this.environment == null) {
            return;
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.remove(PROPERTY_SOURCE_NAME);
        }
    }

    private void validateEnabled() {
        if (this.properties == null || !Boolean.TRUE.equals(this.properties.getEnabled())) {
            throw new IllegalStateException("Postgresql module is disabled via forge-it.modules.postgresql.enabled=false");
        }
    }

    private PostgresqlProperties.Mode requireMode() {
        final PostgresqlProperties.Mode mode = this.properties.getMode();
        if (mode == null) {
            throw new IllegalStateException("forge-it.modules.postgresql.mode must be configured");
        }
        return mode;
    }

    private void initialiseExternal() {
        final String configuredHost = this.properties.getHost();
        if (configuredHost == null || configuredHost.isBlank()) {
            throw new IllegalStateException("forge-it.modules.postgresql.host must be provided for external mode");
        }
        final Integer configuredPort = this.properties.getPort();
        if (configuredPort == null || configuredPort <= 0) {
            throw new IllegalStateException("forge-it.modules.postgresql.port must be provided for external mode");
        }
        this.host = configuredHost;
        this.port = configuredPort;
        this.jdbcUrl = Objects.requireNonNullElseGet(this.properties.getJdbcUrl(),
                () -> "jdbc:postgresql://" + this.host + ":" + this.port + "/" +
                        Objects.requireNonNullElse(this.properties.getDatabase(), "forge-it"));
    }

    private void startInternal() {
        try {
            this.container = new PostgreSQLContainer<>(DockerImageName.parse(
                    Objects.requireNonNullElse(this.properties.getImage(), DEFAULT_IMAGE)))
                    .withDatabaseName(Objects.requireNonNullElse(this.properties.getDatabase(), "forge-it"))
                    .withUsername(Objects.requireNonNullElse(this.properties.getUsername(), "forge-it"))
                    .withPassword(Objects.requireNonNullElse(this.properties.getPassword(), "forge-it"));
            this.container.start();
            this.jdbcUrl = this.container.getJdbcUrl();
            this.host = this.container.getHost();
            this.port = this.container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        } catch (RuntimeException ex) {
            cleanupResources();
            throw new IllegalStateException("Failed to start PostgreSQL Testcontainer", ex);
        }
    }
}
