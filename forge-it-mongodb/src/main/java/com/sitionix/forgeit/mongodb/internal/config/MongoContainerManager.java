package com.sitionix.forgeit.mongodb.internal.config;

import com.mongodb.ConnectionString;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public final class MongoContainerManager implements InitializingBean, DisposableBean {

    private static final String PROPERTY_SOURCE_NAME = "forgeItMongodb";
    private static final String DEFAULT_IMAGE = "mongo:7.0";

    private final ConfigurableEnvironment environment;
    private final MongoProperties properties;

    private MongoDBContainer container;
    private String uri;
    private String host;
    private Integer port;
    private String database;

    @Override
    public void afterPropertiesSet() {
        if (!this.isEnabled()) {
            return;
        }
        final MongoProperties.Mode mode = this.requireMode();
        if (mode == MongoProperties.Mode.EXTERNAL) {
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
        this.uri = null;
        this.host = null;
        this.port = null;
        this.database = null;
    }

    private void publishEnvironment() {
        if (this.environment == null) {
            return;
        }
        if (!StringUtils.hasText(this.uri) || !StringUtils.hasText(this.host) || this.port == null || !StringUtils.hasText(this.database)) {
            throw new IllegalStateException("Mongo container not initialised");
        }

        final MutablePropertySources sources = this.environment.getPropertySources();
        final Map<String, Object> props = Map.of(
                "forge-it.mongodb.connection.uri", this.uri,
                "forge-it.mongodb.connection.host", this.host,
                "forge-it.mongodb.connection.port", this.port,
                "forge-it.mongodb.connection.database", this.database,
                "spring.data.mongodb.uri", this.uri
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

    private boolean isEnabled() {
        return this.properties != null && Boolean.TRUE.equals(this.properties.getEnabled());
    }

    private MongoProperties.Mode requireMode() {
        final MongoProperties.Mode mode = this.properties.getMode();
        if (mode == null) {
            throw new IllegalStateException("forge-it.modules.mongodb.mode must be configured");
        }
        return mode;
    }

    private MongoProperties.Connection requireConnection() {
        final MongoProperties.Connection connection = this.properties.getConnection();
        if (connection == null) {
            throw new IllegalStateException("forge-it.modules.mongodb.connection must be configured");
        }
        return connection;
    }

    private MongoProperties.Container resolveContainer() {
        return this.properties.getContainer();
    }

    private void initialiseExternal() {
        final MongoProperties.Connection connection = this.requireConnection();
        final String configuredUri = connection.getUri();
        if (StringUtils.hasText(configuredUri)) {
            this.uri = configuredUri;
            this.populateFromConnectionString(new ConnectionString(configuredUri), connection);
            return;
        }

        final String configuredHost = connection.getHost();
        if (!StringUtils.hasText(configuredHost)) {
            throw new IllegalStateException("forge-it.modules.mongodb.connection.host must be provided for external mode");
        }
        final Integer configuredPort = connection.getPort();
        if (configuredPort == null || configuredPort <= 0) {
            throw new IllegalStateException("forge-it.modules.mongodb.connection.port must be provided for external mode");
        }
        this.host = configuredHost;
        this.port = configuredPort;
        this.database = Objects.requireNonNullElse(connection.getDatabase(), "forge-it");
        this.uri = "mongodb://" + this.host + ":" + this.port + "/" + this.database;
    }

    private void populateFromConnectionString(final ConnectionString connectionString,
                                              final MongoProperties.Connection connection) {
        final List<String> hosts = connectionString.getHosts();
        if (hosts == null || hosts.isEmpty()) {
            this.host = Objects.requireNonNullElse(connection.getHost(), "localhost");
            this.port = Objects.requireNonNullElse(connection.getPort(), 27017);
        } else {
            final String firstHost = hosts.get(0);
            final String[] hostAndPort = firstHost.split(":");
            this.host = hostAndPort[0];
            this.port = hostAndPort.length > 1
                    ? Integer.parseInt(hostAndPort[1])
                    : 27017;
        }
        this.database = Objects.requireNonNullElse(
                connectionString.getDatabase(),
                Objects.requireNonNullElse(connection.getDatabase(), "forge-it")
        );
    }

    private void startInternal() {
        final MongoProperties.Connection connection = this.requireConnection();
        final MongoProperties.Container containerConfig = this.resolveContainer();
        final String resolvedDatabase = Objects.requireNonNullElse(connection.getDatabase(), "forge-it");
        try {
            this.container = new MongoDBContainer(DockerImageName.parse(
                    Objects.requireNonNullElse(containerConfig == null ? null : containerConfig.getImage(), DEFAULT_IMAGE)));
            this.container.start();
            this.host = this.container.getHost();
            this.port = this.container.getMappedPort(27017);
            this.database = resolvedDatabase;
            this.uri = this.container.getReplicaSetUrl(this.database);
        } catch (final RuntimeException ex) {
            this.cleanupResources();
            throw new IllegalStateException("Failed to start MongoDB Testcontainer", ex);
        }
    }
}
