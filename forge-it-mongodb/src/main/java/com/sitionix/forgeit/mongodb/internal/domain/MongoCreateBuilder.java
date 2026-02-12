package com.sitionix.forgeit.mongodb.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mongodb.internal.config.MongoProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

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
        final String basePath = this.resolveCustomEntityPath();
        this.jsonLoader.setBasePath(basePath);
        final E entity = this.jsonLoader.getFromFile(jsonResourceName, this.entityType);
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

    private String resolveCustomEntityPath() {
        if (this.properties.getPaths() == null
                || this.properties.getPaths().getEntity() == null
                || !StringUtils.hasText(this.properties.getPaths().getEntity().getCustom())) {
            throw new IllegalStateException("forge-it.modules.mongodb.paths.entity.custom must be configured");
        }
        return this.properties.getPaths().getEntity().getCustom().trim();
    }
}
