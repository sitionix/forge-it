package com.sitionix.forgeit.mongodb.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

public final class MongoEntityFetcher implements DbEntityFetcher {

    private final MongoTemplate mongoTemplate;

    public MongoEntityFetcher(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public <E> E reloadById(final DbContract<E> contract, final Object id) {
        return this.mongoTemplate.findById(id, contract.entityType());
    }

    @Override
    public <E> E reloadByIdWithRelations(final DbContract<E> contract, final Object id) {
        return this.reloadById(contract, id);
    }

    @Override
    public <E> List<E> loadAll(final Class<E> entityType) {
        return this.mongoTemplate.findAll(entityType);
    }

    @Override
    public <E> List<E> loadAllWithRelations(final Class<E> entityType) {
        return this.loadAll(entityType);
    }

    @Override
    public <E> List<E> loadAllWithRelations(final DbContract<E> contract) {
        return this.loadAll(contract.entityType());
    }
}
