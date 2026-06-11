package com.sitionix.forgeit.sqlite.internal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public final class SqliteDatabaseManager implements InitializingBean, DisposableBean {

    private static final String PROPERTY_SOURCE_NAME = "forgeItSqlite";
    private static final String DEFAULT_DATABASE_NAME = "forge-it.db";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 0;

    private final ConfigurableEnvironment environment;
    private final SqliteProperties properties;

    private Path temporaryDirectory;
    private String jdbcUrl;
    private String database;

    @Override
    public void afterPropertiesSet() {
        if (!this.isEnabled()) {
            return;
        }
        final SqliteProperties.Mode mode = this.requireMode();
        if (mode == SqliteProperties.Mode.EXTERNAL) {
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
        if (this.temporaryDirectory != null) {
            try {
                Files.walk(this.temporaryDirectory)
                        .sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (final IOException ignored) {
                            }
                        });
            } catch (final IOException ignored) {
            }
            this.temporaryDirectory = null;
        }
        this.jdbcUrl = null;
        this.database = null;
    }

    private void publishEnvironment() {
        if (this.environment == null) {
            return;
        }
        if (this.jdbcUrl == null || this.database == null) {
            throw new IllegalStateException("SQLite database not initialised");
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        final SqliteProperties.Connection connection = this.requireConnection();
        final Map<String, Object> props = Map.of(
                "forge-it.sqlite.connection.jdbc-url", this.jdbcUrl,
                "forge-it.sqlite.connection.database", this.database,
                "forge-it.sqlite.connection.host", Objects.requireNonNullElse(connection.getHost(), DEFAULT_HOST),
                "forge-it.sqlite.connection.port", Objects.requireNonNullElse(connection.getPort(), DEFAULT_PORT),
                "forge-it.sqlite.connection.username", Objects.requireNonNullElse(connection.getUsername(), ""),
                "forge-it.sqlite.connection.password", Objects.requireNonNullElse(connection.getPassword(), ""),
                "spring.datasource.url", this.jdbcUrl,
                "spring.datasource.driver-class-name", "org.sqlite.JDBC",
                "spring.jpa.database-platform", "org.hibernate.community.dialect.SQLiteDialect"
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

    private SqliteProperties.Mode requireMode() {
        final SqliteProperties.Mode mode = this.properties.getMode();
        if (mode == null) {
            throw new IllegalStateException("forge-it.modules.sqlite.mode must be configured");
        }
        return mode;
    }

    private SqliteProperties.Connection requireConnection() {
        final SqliteProperties.Connection connection = this.properties.getConnection();
        if (connection == null) {
            throw new IllegalStateException("forge-it.modules.sqlite.connection must be configured");
        }
        return connection;
    }

    private void initialiseExternal() {
        final SqliteProperties.Connection connection = this.requireConnection();
        final String configuredUrl = connection.getJdbcUrl();
        if (StringUtils.hasText(configuredUrl)) {
            this.jdbcUrl = configuredUrl.trim();
            this.database = this.databaseFromJdbcUrl(this.jdbcUrl);
            return;
        }
        final String configuredDatabase = connection.getDatabase();
        if (!StringUtils.hasText(configuredDatabase)) {
            throw new IllegalStateException(
                    "forge-it.modules.sqlite.connection.database or jdbc-url must be provided for external mode");
        }
        this.database = configuredDatabase.trim();
        this.jdbcUrl = "jdbc:sqlite:" + this.database;
    }

    private void startInternal() {
        try {
            this.temporaryDirectory = Files.createTempDirectory("forge-it-sqlite-");
            final Path databasePath = this.temporaryDirectory.resolve(DEFAULT_DATABASE_NAME);
            this.database = databasePath.toAbsolutePath().toString();
            this.jdbcUrl = "jdbc:sqlite:" + this.database;
        } catch (final IOException ex) {
            this.cleanupResources();
            throw new IllegalStateException("Failed to initialise SQLite database", ex);
        }
    }

    private String databaseFromJdbcUrl(final String value) {
        if (value.startsWith("jdbc:sqlite:")) {
            final String databasePart = value.substring("jdbc:sqlite:".length());
            if (StringUtils.hasText(databasePart)) {
                return databasePart;
            }
        }
        return value;
    }
}
