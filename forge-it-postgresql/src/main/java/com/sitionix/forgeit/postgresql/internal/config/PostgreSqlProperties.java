package com.sitionix.forgeit.postgresql.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for PostgreSQL settings exposed via {@code forge-it.modules.postgresql}.
 */
@Data
@Component
@ConfigurationProperties(prefix = PostgreSqlProperties.PROPERTY_PREFIX)
public final class PostgreSqlProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.postgresql";

    private Boolean enabled;
    private Mode mode;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    private String image;
    private String jdbcUrl;
    private String template;

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }
}
