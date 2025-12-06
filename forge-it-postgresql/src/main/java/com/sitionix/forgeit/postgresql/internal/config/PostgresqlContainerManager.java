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
        this.validateEnabled();
        final PostgresqlProperties.Mode mode = this.requireMode();
        if (mode == PostgresqlProperties.Mode.EXTERNAL) {
            this.initialiseExternal();
        } else {
            this.startInternal();
        }
        this.publishEnvironment();
    }

    @Override
    public void destroy() {
        this.cleanupResources();
    }

    private void cleanupResources() {
        this.removeEnvironment();
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
                "forge-it.postgresql.connection.jdbc-url", this.jdbcUrl,
                "forge-it.postgresql.connection.port", this.port,
                "forge-it.postgresql.connection.host", this.host,
                "forge-it.postgresql.connection.database", Objects.requireNonNullElse(this.properties.getConnection().getDatabase(), "forge-it"),
                "forge-it.postgresql.connection.username", Objects.requireNonNullElse(this.properties.getConnection().getUsername(), "forge-it"),
                "forge-it.postgresql.connection.password", Objects.requireNonNullElse(this.properties.getConnection().getPassword(), "forge-it")
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
        final String configuredHost = this.properties.getConnection().getHost();
        if (configuredHost == null || configuredHost.isBlank()) {
            throw new IllegalStateException("forge-it.modules.postgresql.connection.host must be provided for external mode");
        }
        final Integer configuredPort = this.properties.getConnection().getPort();
        if (configuredPort == null || configuredPort <= 0) {
            throw new IllegalStateException("forge-it.modules.postgresql.connection.port must be provided for external mode");
        }
        this.host = configuredHost;
        this.port = configuredPort;
        this.jdbcUrl = Objects.requireNonNullElseGet(this.properties.getConnection().getJdbcUrl(),
                () -> "jdbc:postgresql://" + this.host + ":" + this.port + "/" +
                        Objects.requireNonNullElse(this.properties.getConnection().getDatabase(), "forge-it"));
    }

    private void startInternal() {
        try {
            this.container = new PostgreSQLContainer<>(DockerImageName.parse(
                    Objects.requireNonNullElse(this.properties.getContainer().getImage(), DEFAULT_IMAGE)))
                    .withDatabaseName(Objects.requireNonNullElse(this.properties.getConnection().getDatabase(), "forge-it"))
                    .withUsername(Objects.requireNonNullElse(this.properties.getConnection().getUsername(), "forge-it"))
                    .withPassword(Objects.requireNonNullElse(this.properties.getConnection().getPassword(), "forge-it"));
            this.container.start();
            this.jdbcUrl = this.container.getJdbcUrl();
            this.host = this.container.getHost();
            this.port = this.container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        } catch (final RuntimeException ex) {
            this.cleanupResources();
            throw new IllegalStateException("Failed to start PostgreSQL Testcontainer", ex);
        }
    }
}
