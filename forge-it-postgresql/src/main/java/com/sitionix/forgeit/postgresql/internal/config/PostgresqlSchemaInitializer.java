package com.sitionix.forgeit.postgresql.internal.config;

import com.sitionix.forgeit.domain.executor.SqlScriptExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostgresqlSchemaInitializer {

    @SuppressWarnings("unused")
    private final PostgresqlContainerManager containerManager;
    private final SqlScriptExecutor sqlScriptExecutor;
    private final DataSource dataSource;
    private final PostgresqlProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchema() {
        if (this.properties.getPaths() == null ||
                this.properties.getPaths().getDdl().getPath() == null ||
                this.properties.getPaths().getDdl().getPath().isBlank()) {
            log.debug("PostgreSQL DDL path is not configured, skipping schema initialization");
            return;
        }

        final String basePath = this.properties.getPaths().getDdl().getPath().trim();
        log.info("Initializing PostgreSQL schema from path: {}", basePath);

        try {
            this.sqlScriptExecutor.executeAllForDataSource(this.dataSource, this.properties.getPaths().getDdl().getPath());
            log.info("PostgreSQL schema initialization completed successfully");
        } catch (final Exception ex) {
            log.error("PostgreSQL schema initialization failed", ex);
            throw new IllegalStateException("Failed to initialize PostgreSQL schema from " + basePath, ex);
        }
    }
}
