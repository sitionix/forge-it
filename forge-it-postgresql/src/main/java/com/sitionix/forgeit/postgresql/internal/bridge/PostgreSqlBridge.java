package com.sitionix.forgeit.postgresql.internal.bridge;

import com.sitionix.forgeit.postgresql.internal.config.PostgreSqlContainerManager;
import com.sitionix.forgeit.postgresql.internal.config.PostgreSqlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Simple bridge exposing PostgreSQL details to consumers.
 */
@RequiredArgsConstructor
@Component
public class PostgreSqlBridge {

    private final PostgreSqlContainerManager containerManager;
    private final PostgreSqlProperties properties;

    public String template() {
        return Objects.requireNonNullElse(this.properties.getTemplate(), "postgresql-template");
    }

    public String jdbcUrl() {
        return this.containerManager.getJdbcUrl();
    }
}
