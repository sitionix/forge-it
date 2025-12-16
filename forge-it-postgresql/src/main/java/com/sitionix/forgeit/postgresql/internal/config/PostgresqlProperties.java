package com.sitionix.forgeit.postgresql.internal.config;

import com.sitionix.forgeit.domain.model.sql.RelationalModuleProperties;
import com.sitionix.forgeit.postgresql.internal.domain.GraphTxPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for PostgreSQL settings exposed via {@code forge-it.modules.postgresql}.
 */
@Data
@Component
@ConfigurationProperties(prefix = PostgresqlProperties.PROPERTY_PREFIX)
public final class PostgresqlProperties implements RelationalModuleProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.postgresql";

    /**
     * Controls whether the PostgreSQL module is active.
     */
    private Boolean enabled;

    /**
     * Defines how PostgreSQL is provided: internally via Testcontainers or externally.
     */
    private RelationalModuleProperties.Mode mode;

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
     * Maps from "forge-it.modules.postgresql.paths".
     */
    private Paths paths;

    /**
     * Transaction policy used by graph executor.
     */
    private GraphTxPolicy txPolicy = GraphTxPolicy.REQUIRES_NEW;

    @Data
    public static final class Container {
        /**
         * Docker image for PostgreSQL container (internal mode).
         */
        private String image;
        // можна буде додати template, reuse, timeout тощо
    }

    @Data
    public static final class Connection implements RelationalModuleProperties.Connection {

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
    public static final class Paths implements RelationalModuleProperties.Paths {

        /**
         * Path group for DDL scripts.
         */
        private Ddl ddl;

        /**
         * Paths for JSON entity fixtures.
         */
        private Entity entity;

        @Data
        public static final class Ddl implements RelationalModuleProperties.Paths.Ddl {
            /**
             * Base path for schema-related SQL scripts.
             */
            private String path;
        }

        @Data
        public static final class Entity implements RelationalModuleProperties.Paths.Entity {
            /**
             * Base path for default JSON payloads.
             */
            private String defaults;

            /**
             * Base path for custom JSON payloads (перевизначення дефолтів).
             */
            private String custom;
        }
    }
}
