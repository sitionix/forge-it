package com.sitionix.forgeit.postgresql.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for PostgreSQL settings exposed via {@code forge-it.modules.postgresql}.
 */
@Data
@Component
@ConfigurationProperties(prefix = PostgresqlProperties.PROPERTY_PREFIX)
public final class PostgresqlProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.postgresql";

    /**
     * Controls whether the PostgreSQL module is active.
     */
    private Boolean enabled;

    /**
     * Defines how PostgreSQL is provided: internally via Testcontainers or externally.
     */
    private Mode mode;

    /**
     * Container-related configuration (used for internal mode).
     * Maps from "forge-it.modules.postgresql.container".
     */
    private Container container;

    /**
     * JDBC connection-related configuration.
     * Maps from "forge-it.modules.postgresql.connection".
     */
    private Connection connection;

    /**
     * DDL / SQL scripts configuration (root path for SQL files).
     * Maps from "forge-it.modules.postgresql.ddl-path".
     */
    private Paths paths;

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }

    @Data
    public static final class Container {

        /**
         * Docker image for PostgreSQL container (internal mode).
         */
        private String image;
        // можна буде додати template, reuse, timeout тощо
    }

    @Data
    public static final class Connection {

        /**
         * Database name.
         */
        private String database;

        /**
         * Username for the database.
         */
        private String username;

        /**
         * Password for the database.
         */
        private String password;

        /**
         * Host for external mode (ігнорується для internal, якщо ти так вирішиш у логіці).
         */
        private String host;

        /**
         * Port for external mode (ігнорується для internal, якщо ти так вирішиш у логіці).
         */
        private Integer port;

        /**
         * Optional explicit JDBC URL. If set, it may override host/port/database.
         */
        private String jdbcUrl;
    }

    @Data
    public static final class Paths {

        /**
         * Base path for SQL scripts (e.g. "db/postgresql" or "classpath:db/postgresql").
         */
        private Ddl ddl;

        private Entity entity;


        @Data
        public static final class Entity {
            private String defaults;
            private String custom;
        }

        /**
         * Path for schema-related SQL scripts.
         */
        @Data
        public static final class Ddl {
            private String path;
        }
    }
}
