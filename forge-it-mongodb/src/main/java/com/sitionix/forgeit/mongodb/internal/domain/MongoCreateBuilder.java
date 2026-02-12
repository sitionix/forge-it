package com.sitionix.forgeit.mongodb.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mongodb.internal.config.MongoProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for MongoDB document creation without contract declarations.
 */
public final class MongoCreateBuilder<E> {

    private final Class<E> entityType;
    private final MongoTemplate mongoTemplate;
    private final JsonLoader jsonLoader;
    private final MongoProperties properties;
    private final DbContract<E> contract;

    public MongoCreateBuilder(final Class<E> entityType,
                              final MongoTemplate mongoTemplate,
                              final JsonLoader jsonLoader,
                              final MongoProperties properties) {
        this.entityType = entityType;
        this.mongoTemplate = mongoTemplate;
        this.jsonLoader = jsonLoader;
        this.properties = properties;
        this.contract = new MongoEntityContract<>(entityType);
    }

    public DbEntityHandle<E> body(final String jsonResourceName) {
        if (!StringUtils.hasText(jsonResourceName)) {
            throw new IllegalArgumentException("Json resource name must not be blank");
        }
        final E entity = this.loadEntityFromConfiguredPaths(jsonResourceName);
        return this.persistEntity(entity);
    }

    public DbEntityHandle<E> body(final E entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity body must not be null");
        }
        return this.persistEntity(entity);
    }

    private DbEntityHandle<E> persistEntity(final E entity) {
        final E persisted = this.mongoTemplate.save(entity);
        return new DbEntityHandle<>(persisted, this.contract, this.mongoTemplate::save);
    }

    private E loadEntityFromConfiguredPaths(final String jsonResourceName) {
        RuntimeException lastException = null;
        for (final String basePath : this.resolveEntitySearchPaths()) {
            try {
                this.jsonLoader.setBasePath(basePath);
                return this.jsonLoader.getFromFile(jsonResourceName, this.entityType);
            } catch (final RuntimeException ex) {
                lastException = ex;
            }
        }
        throw new IllegalStateException("Unable to load Mongo entity fixture '" + jsonResourceName + "'", lastException);
    }

    private List<String> resolveEntitySearchPaths() {
        if (this.properties.getPaths() == null || this.properties.getPaths().getEntity() == null) {
            throw new IllegalStateException("forge-it.modules.mongodb.paths.entity must be configured");
        }
        final List<String> basePaths = new ArrayList<>();
        final String customPath = this.properties.getPaths().getEntity().getCustom();
        if (StringUtils.hasText(customPath)) {
            basePaths.add(customPath.trim());
        }
        final String defaultsPath = this.properties.getPaths().getEntity().getDefaults();
        if (StringUtils.hasText(defaultsPath)) {
            final String trimmed = defaultsPath.trim();
            if (!basePaths.contains(trimmed)) {
                basePaths.add(trimmed);
            }
        }
        if (basePaths.isEmpty()) {
            throw new IllegalStateException(
                    "At least one of forge-it.modules.mongodb.paths.entity.custom/defaults must be configured");
        }
        return basePaths;
    }
}
