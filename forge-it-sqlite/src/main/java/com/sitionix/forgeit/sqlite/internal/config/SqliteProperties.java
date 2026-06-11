package com.sitionix.forgeit.sqlite.internal.config;

import com.sitionix.forgeit.domain.model.sql.RelationalModuleProperties;
import com.sitionix.forgeit.sqlite.internal.domain.GraphTxPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for SQLite settings exposed via {@code forge-it.modules.sqlite}.
 */
@Data
@Component
@ConfigurationProperties(prefix = SqliteProperties.PROPERTY_PREFIX)
public final class SqliteProperties implements RelationalModuleProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.sqlite";

    /**
     * Controls whether the SQLite module is active.
     */
    private Boolean enabled;

    /**
     * Defines how SQLite is provided: internally as a temporary file database or externally.
     */
    private RelationalModuleProperties.Mode mode;

    /**
     * JDBC connection-related configuration.
     * Maps from "forge-it.modules.sqlite.connection".
     */
    private Connection connection;

    /**
     * DDL / SQL scripts configuration (root path for SQL files).
     * Maps from "forge-it.modules.sqlite.paths".
     */
    private Paths paths;

    /**
     * Transaction policy used by graph executor.
     */
    private GraphTxPolicy txPolicy = GraphTxPolicy.REQUIRES_NEW;

    @Data
    public static final class Connection implements RelationalModuleProperties.Connection {

        /**
         * Database file path. For external mode this is used when jdbcUrl is not provided.
         */
        private String database;

        /**
         * Username placeholder for compatibility with relational connection settings.
         */
        private String username;

        /**
         * Password placeholder for compatibility with relational connection settings.
         */
        private String password;

        /**
         * Host placeholder for compatibility with relational connection settings.
         */
        private String host;

        /**
         * Port placeholder for compatibility with relational connection settings.
         */
        private Integer port;

        /**
         * Optional explicit JDBC URL. If set, it overrides database.
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
             * Base path for custom JSON payloads.
             */
            private String custom;
        }
    }
}
