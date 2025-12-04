package com.sitionix.forgeit.postgresql.internal.repository;

import com.sitionix.forgeit.domain.contract.DbGraphBuilder;
import com.sitionix.forgeit.postgresql.internal.config.PostgresqlProperties;
import com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Simple bridge exposing PostgreSQL details to consumers.
 */
@RequiredArgsConstructor
@Component
public class PostgresForge {

    private final PostgresqlProperties properties;

    public DbGraphBuilder create() {
        return new PostgresGraphBuilder();
    }
}
