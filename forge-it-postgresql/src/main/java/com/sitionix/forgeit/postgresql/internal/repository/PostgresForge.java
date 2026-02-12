package com.sitionix.forgeit.postgresql.internal.repository;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.assertion.DbEntitiesAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertionBuilderFactory;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.model.sql.DbRetrieveFactory;
import com.sitionix.forgeit.domain.model.sql.DbRetriever;
import com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphBuilder;
import com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Simple bridge exposing PostgreSQL details to consumers.
 */
@Component
public class PostgresForge {

    private final DbEntityFactory entityFactory;

    private final PostgresGraphExecutor graphExecutor;

    private final DbRetrieveFactory retrieveFactory;

    private final DbCleaner dbCleaner;

    private final DbEntityAssertionBuilderFactory assertionBuilderFactory;

    public PostgresForge(final DbEntityFactory entityFactory,
                         final PostgresGraphExecutor graphExecutor,
                         final DbRetrieveFactory retrieveFactory,
                         @Qualifier("jpaDbCleaner") final DbCleaner dbCleaner,
                         final DbEntityAssertionBuilderFactory assertionBuilderFactory) {
        this.entityFactory = entityFactory;
        this.graphExecutor = graphExecutor;
        this.retrieveFactory = retrieveFactory;
        this.dbCleaner = dbCleaner;
        this.assertionBuilderFactory = assertionBuilderFactory;
    }

    public DbGraphBuilder create() {
        return new PostgresGraphBuilder(this.entityFactory,
                this.graphExecutor);
    }

    /**
     * Create a retriever for the given entity type (for ad-hoc verification or assertions).
     */
    public <E> DbRetriever<E> get(final Class<E> entityClass) {
        return this.retrieveFactory.forClass(entityClass);
    }

    /**
     * Retrieve all entities registered for the given contract.
     */
    public <E> List<E> get(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return this.retrieveFactory.forClass(contract.entityType()).getAll();
    }

    /**
     * Clear all data for the supplied contracts (typically invoked by cleanup listeners).
     */
    public void clearAllData(final List<DbContract<?>> contracts) {
        this.dbCleaner.clearTables(contracts);
    }

    /**
     * Start an assertion workflow for a single entity handle.
     */
    public <E> DbEntityAssertionBuilder<E> assertEntity(final DbEntityHandle<E> handle) {
        if (handle == null) {
            throw new IllegalArgumentException("DbEntityHandle must not be null");
        }
        return this.assertionBuilderFactory.forEntity(handle);
    }

    /**
     * Start an assertion workflow for all entities stored for the given contract.
     */
    public <E> DbEntitiesAssertionBuilder<E> assertEntities(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return this.assertionBuilderFactory.forContract(contract);
    }

    /**
     * Start an assertion workflow for all entities of the given type.
     */
    public <E> DbEntitiesAssertionBuilder<E> assertEntities(final Class<E> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type must not be null");
        }
        return this.assertionBuilderFactory.forEntityType(entityType);
    }
}
