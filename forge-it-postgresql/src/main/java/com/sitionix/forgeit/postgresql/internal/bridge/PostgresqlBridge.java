package com.sitionix.forgeit.postgresql.internal.bridge;

import com.sitionix.forgeit.postgresql.internal.config.PostgresqlContainerManager;
import com.sitionix.forgeit.postgresql.internal.config.PostgresqlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Simple bridge exposing PostgreSQL details to consumers.
 */
@RequiredArgsConstructor
@Component
public class PostgresqlBridge {

    private final PostgresqlContainerManager containerManager;
    private final PostgresqlProperties properties;

    public String template() {
        return Objects.requireNonNullElse(this.properties.getTemplate(), "postgresql-template");
    }

    public String jdbcUrl() {
        return this.containerManager.getJdbcUrl();
    }
}
