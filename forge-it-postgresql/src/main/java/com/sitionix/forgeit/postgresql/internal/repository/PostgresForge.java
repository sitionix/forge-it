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
import java.util.Iterator;
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

    public <E> List<E> get(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return this.retrieveFactory.forClass(contract.entityType()).getAll();
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

    public <E> EntitiesAssertionBuilder<E> assertEntities(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return new EntitiesAssertionBuilder<>(contract, this.retrieveFactory, this.entityAssertions);
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

    public static final class EntitiesAssertionBuilder<E> {
        private final DbContract<E> contract;
        private final DbRetrieveFactory retrieveFactory;
        private final DbEntityAssertions entityAssertions;
        private final List<String> fieldsToIgnore;

        private EntitiesAssertionBuilder(final DbContract<E> contract,
                                         final DbRetrieveFactory retrieveFactory,
                                         final DbEntityAssertions entityAssertions) {
            this.contract = contract;
            this.retrieveFactory = retrieveFactory;
            this.entityAssertions = entityAssertions;
            this.fieldsToIgnore = new ArrayList<>();
        }

        public EntitiesAssertionBuilder<E> ignoreFields(final String... fields) {
            if (fields != null) {
                for (final String field : fields) {
                    if (field != null) {
                        this.fieldsToIgnore.add(field);
                    }
                }
            }
            return this;
        }

        public void matchAllWithJsons(final String... jsonResourceNames) {
            this.matchAll(jsonResourceNames, false);
        }

        public void matchAllWithDefaultJsons(final String... jsonResourceNames) {
            this.matchAll(jsonResourceNames, true);
        }

        public void matchExactlyWithJsons(final String... jsonResourceNames) {
            this.matchExactly(jsonResourceNames, false);
        }

        public void matchExactlyWithDefaultJsons(final String... jsonResourceNames) {
            this.matchExactly(jsonResourceNames, true);
        }

        private void matchAll(final String[] jsonResourceNames, final boolean useDefaultJson) {
            if (jsonResourceNames == null) {
                throw new IllegalArgumentException("Json resource names must not be null");
            }
            final List<E> entities = this.retrieveFactory.forClass(this.contract.entityType()).getAll();
            final List<E> remaining = new ArrayList<>(entities);
            final String[] ignoreFields = this.fieldsToIgnore.toArray(new String[0]);

            for (final String jsonResourceName : jsonResourceNames) {
                boolean matched = false;
                AssertionError lastError = null;

                for (final Iterator<E> iterator = remaining.iterator(); iterator.hasNext(); ) {
                    final E entity = iterator.next();
                    final DbEntityHandle<E> handle = new DbEntityHandle<>(entity, this.contract);
                    try {
                        if (useDefaultJson) {
                            this.entityAssertions.assertEntityMatchesDefaultJson(handle, jsonResourceName, ignoreFields);
                        } else {
                            this.entityAssertions.assertEntityMatchesJson(handle, jsonResourceName, ignoreFields);
                        }
                        iterator.remove();
                        matched = true;
                        break;
                    } catch (final AssertionError e) {
                        lastError = e;
                    }
                }

                if (!matched) {
                    if (lastError != null) {
                        throw new AssertionError(String.format("No entity matched json '%s' for contract %s",
                                jsonResourceName,
                                this.contract.entityType().getSimpleName()), lastError);
                    }
                    throw new AssertionError(String.format("No entity matched json '%s' for contract %s",
                            jsonResourceName,
                            this.contract.entityType().getSimpleName()));
                }
            }
        }

        private void matchExactly(final String[] jsonResourceNames, final boolean useDefaultJson) {
            if (jsonResourceNames == null) {
                throw new IllegalArgumentException("Json resource names must not be null");
            }

            final List<E> entities = this.retrieveFactory.forClass(this.contract.entityType()).getAll();
            if (entities.size() != jsonResourceNames.length) {
                throw new AssertionError(String.format("Entity count mismatch for contract %s: expected %d but was %d",
                        this.contract.entityType().getSimpleName(),
                        jsonResourceNames.length,
                        entities.size()));
            }

            this.matchAll(jsonResourceNames, useDefaultJson);
        }
    }
}
