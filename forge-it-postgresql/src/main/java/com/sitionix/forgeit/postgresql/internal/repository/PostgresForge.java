package com.sitionix.forgeit.postgresql.internal.repository;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.model.sql.DbRetrieveFactory;
import com.sitionix.forgeit.domain.model.sql.DbRetriever;
import com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphBuilder;
import com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple bridge exposing PostgreSQL details to consumers.
 */
@RequiredArgsConstructor
@Component
public class PostgresForge {

    private final DbEntityFactory entityFactory;

    private final PostgresGraphExecutor graphExecutor;

    private final DbRetrieveFactory retrieveFactory;

    private final DbCleaner dbCleaner;

    private final DbEntityAssertions entityAssertions;

    public DbGraphBuilder create() {
        return new PostgresGraphBuilder(this.entityFactory,
                this.graphExecutor);
    }

    public <E> DbRetriever<E> get(final Class<E> entityClass) {
        return this.retrieveFactory.forClass(entityClass);
    }

    public void clearAllData(final List<DbContract<?>> contracts) {
        this.dbCleaner.clearTables(contracts);
    }

    public <E> EntityAssertionBuilder<E> assertEntity(final DbEntityHandle<E> handle) {
        if (handle == null) {
            throw new IllegalArgumentException("DbEntityHandle must not be null");
        }
        return new EntityAssertionBuilder<>(handle, this.entityAssertions);
    }

    public static final class EntityAssertionBuilder<E> {
        private final DbEntityHandle<E> handle;
        private final DbEntityAssertions entityAssertions;
        private final List<String> fieldsToIgnore;
        private String jsonResourceName;
        private boolean useDefaultJson;

        private EntityAssertionBuilder(final DbEntityHandle<E> handle,
                                       final DbEntityAssertions entityAssertions) {
            this.handle = handle;
            this.entityAssertions = entityAssertions;
            this.fieldsToIgnore = new ArrayList<>();
        }

        public EntityAssertionBuilder<E> withJson(final String jsonResourceName) {
            this.jsonResourceName = jsonResourceName;
            this.useDefaultJson = false;
            return this;
        }

        public EntityAssertionBuilder<E> withDefaultJson(final String jsonResourceName) {
            this.jsonResourceName = jsonResourceName;
            this.useDefaultJson = true;
            return this;
        }

        public EntityAssertionBuilder<E> ignoreFields(final String... fields) {
            if (fields != null) {
                for (final String field : fields) {
                    if (field != null) {
                        this.fieldsToIgnore.add(field);
                    }
                }
            }
            return this;
        }

        public void assertMatches() {
            if (this.useDefaultJson) {
                this.entityAssertions.assertEntityMatchesDefaultJson(
                        this.handle,
                        this.jsonResourceName,
                        this.fieldsToIgnore.toArray(new String[0])
                );
                return;
            }
            this.entityAssertions.assertEntityMatchesJson(
                    this.handle,
                    this.jsonResourceName,
                    this.fieldsToIgnore.toArray(new String[0])
            );
        }

        public void assertMatchesStrict() {
            if (this.useDefaultJson) {
                this.entityAssertions.assertEntityMatchesDefaultJsonStrict(
                        this.handle,
                        this.jsonResourceName,
                        this.fieldsToIgnore.toArray(new String[0])
                );
                return;
            }
            this.entityAssertions.assertEntityMatchesJsonStrict(
                    this.handle,
                    this.jsonResourceName,
                    this.fieldsToIgnore.toArray(new String[0])
            );
        }
    }
}
