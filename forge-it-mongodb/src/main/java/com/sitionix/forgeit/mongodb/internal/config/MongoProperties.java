package com.sitionix.forgeit.mongodb.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for MongoDB settings exposed via {@code forge-it.modules.mongodb}.
 */
@Data
@Component
@ConfigurationProperties(prefix = MongoProperties.PROPERTY_PREFIX)
public final class MongoProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.mongodb";

    private Boolean enabled;

    private Mode mode;

    private Container container;

    private Connection connection;

    private Paths paths;

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }

    @Data
    public static final class Container {

        private String image;
    }

    @Data
    public static final class Connection {

        private String uri;

        private String host;

        private Integer port;

        private String database;
    }

    @Data
    public static final class Paths {

        private Entity entity;

        @Data
        public static final class Entity {

            private String defaults;

            private String custom;

            private String expected;
        }
    }
}
