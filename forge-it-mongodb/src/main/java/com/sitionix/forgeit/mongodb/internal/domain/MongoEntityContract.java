package com.sitionix.forgeit.mongodb.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbDependency;
import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;

import java.util.List;

final class MongoEntityContract<E> implements DbContract<E> {

    private final Class<E> entityType;

    MongoEntityContract(final Class<E> entityType) {
        this.entityType = entityType;
    }

    @Override
    public Class<E> entityType() {
        return this.entityType;
    }

    @Override
    public List<DbDependency<E, ?>> dependencies() {
        return List.of();
    }

    @Override
    public String defaultJsonResourceName() {
        return null;
    }

    @Override
    public CleanupPolicy cleanupPolicy() {
        return CleanupPolicy.NONE;
    }
}
