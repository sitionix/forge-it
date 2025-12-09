package com.sitionix.forgeit.postgresql.internal.repository;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.model.sql.DbRetrieveFactory;
import com.sitionix.forgeit.domain.model.sql.DbRetriever;
import com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphBuilder;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Simple bridge exposing PostgreSQL details to consumers.
 */
@RequiredArgsConstructor
@Component
public class PostgresForge {

    private final DbEntityFactory entityFactory;

    private final EntityManager entityManager;

    private final DbRetrieveFactory retrieveFactory;

    private final DbCleaner dbCleaner;

    public DbGraphBuilder create() {
        return new PostgresGraphBuilder(this.entityFactory,
                this.entityManager);
    }

    public <E> DbRetriever<E> get(final Class<E> entityClass) {
        return this.retrieveFactory.forClass(entityClass);
    }

    public void clearAllData(final List<DbContract<?>> contracts) {
        this.dbCleaner.clearTables(contracts);
    }
}
