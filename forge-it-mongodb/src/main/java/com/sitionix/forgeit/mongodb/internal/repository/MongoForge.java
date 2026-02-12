package com.sitionix.forgeit.mongodb.internal.repository;

import com.sitionix.forgeit.application.validator.EntitiesAssertionBuilder;
import com.sitionix.forgeit.application.validator.EntityAssertionBuilder;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.assertion.DbEntitiesAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertionBuilder;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.domain.model.sql.DbRetriever;
import com.sitionix.forgeit.mongodb.internal.cleaner.MongoCollectionCleaner;
import com.sitionix.forgeit.mongodb.internal.config.MongoProperties;
import com.sitionix.forgeit.mongodb.internal.domain.MongoCreateBuilder;
import com.sitionix.forgeit.mongodb.internal.domain.MongoEntityAssertions;
import com.sitionix.forgeit.mongodb.internal.domain.MongoEntityFetcher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bridge exposing MongoDB details to consumers.
 */
@Component
public class MongoForge {

    private final MongoTemplate mongoTemplate;
    private final JsonLoader jsonLoader;
    private final MongoProperties properties;
    private final MongoEntityAssertions entityAssertions;
    private final MongoEntityFetcher entityFetcher;
    private final MongoCollectionCleaner dbCleaner;

    public MongoForge(final MongoTemplate mongoTemplate,
                      final JsonLoader jsonLoader,
                      final MongoProperties properties,
                      final MongoEntityAssertions entityAssertions,
                      final MongoEntityFetcher entityFetcher,
                      final MongoCollectionCleaner dbCleaner) {
        this.mongoTemplate = mongoTemplate;
        this.jsonLoader = jsonLoader;
        this.properties = properties;
        this.entityAssertions = entityAssertions;
        this.entityFetcher = entityFetcher;
        this.dbCleaner = dbCleaner;
    }

    public <E> MongoCreateBuilder<E> create(final Class<E> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity type must not be null");
        }
        return new MongoCreateBuilder<>(entityClass, this.mongoTemplate, this.jsonLoader, this.properties);
    }

    public <E> DbRetriever<E> get(final Class<E> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity type must not be null");
        }
        return new DbRetriever<>() {
            @Override
            public E getById(final Object id) {
                return MongoForge.this.mongoTemplate.findById(id, entityClass);
            }

            @Override
            public List<E> getAll() {
                return MongoForge.this.mongoTemplate.findAll(entityClass);
            }
        };
    }

    public void clearAllData(final List<DbContract<?>> contracts) {
        this.dbCleaner.clearTables(contracts);
    }

    public <E> DbEntityAssertionBuilder<E> assertEntity(final DbEntityHandle<E> handle) {
        if (handle == null) {
            throw new IllegalArgumentException("DbEntityHandle must not be null");
        }
        return new EntityAssertionBuilder<>(handle, this.entityAssertions, this.entityFetcher);
    }

    public <E> DbEntitiesAssertionBuilder<E> assertEntities(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return new EntitiesAssertionBuilder<>(contract,
                contract.entityType(),
                this.entityAssertions,
                this.entityFetcher);
    }

    public <E> DbEntitiesAssertionBuilder<E> assertEntities(final Class<E> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type must not be null");
        }
        return new EntitiesAssertionBuilder<>(null,
                entityType,
                this.entityAssertions,
                this.entityFetcher);
    }
}
