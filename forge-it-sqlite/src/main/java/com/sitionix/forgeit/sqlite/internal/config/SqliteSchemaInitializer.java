package com.sitionix.forgeit.sqlite.internal.config;

import com.sitionix.forgeit.domain.executor.SqlScriptExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
@Component
public class SqliteSchemaInitializer {

    @SuppressWarnings("unused")
    private final SqliteDatabaseManager databaseManager;
    private final SqlScriptExecutor sqlScriptExecutor;
    @Qualifier("sqliteDataSource")
    private final DataSource dataSource;
    private final SqliteProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchema() {
        if (this.properties.getPaths() == null ||
                this.properties.getPaths().getDdl().getPath() == null ||
                this.properties.getPaths().getDdl().getPath().isBlank()) {
            log.debug("SQLite DDL path is not configured, skipping schema initialization");
            return;
        }

        final String basePath = this.properties.getPaths().getDdl().getPath().trim();
        log.info("Initializing SQLite schema from path: {}", basePath);

        try {
            this.sqlScriptExecutor.executeAllForDataSource(this.dataSource, basePath);
            log.info("SQLite schema initialization completed successfully");
        } catch (final Exception ex) {
            log.error("SQLite schema initialization failed", ex);
            throw new IllegalStateException("Failed to initialize SQLite schema from " + basePath, ex);
        }
    }
}
