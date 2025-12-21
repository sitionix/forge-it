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

    /**
     * Start an assertion workflow for all entities stored for the given contract.
     */
    public <E> EntitiesAssertionBuilder<E> assertEntities(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return new EntitiesAssertionBuilder<>(contract, contract.entityType(), this.retrieveFactory, this.entityAssertions);
    }

    /**
     * Start an assertion workflow for all entities of the given type.
     */
    public <E> EntitiesAssertionBuilder<E> assertEntities(final Class<E> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type must not be null");
        }
        return new EntitiesAssertionBuilder<>(null, entityType, this.retrieveFactory, this.entityAssertions);
    }

    /**
     * Builder for asserting a single entity against a JSON fixture.
     */
    public static final class EntityAssertionBuilder<E> {
        private final DbEntityHandle<E> handle;
        private final DbEntityAssertions entityAssertions;
        private final List<String> fieldsToIgnore;
        private String jsonResourceName;

        private EntityAssertionBuilder(final DbEntityHandle<E> handle,
                                       final DbEntityAssertions entityAssertions) {
            this.handle = handle;
            this.entityAssertions = entityAssertions;
            this.fieldsToIgnore = new ArrayList<>();
        }

        /**
         * Select a JSON fixture name from the custom entity fixtures path.
         */
        public EntityAssertionBuilder<E> withJson(final String jsonResourceName) {
            this.jsonResourceName = jsonResourceName;
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

        /**
         * Compare only the fields present in the JSON fixture.
         */
        public void assertMatches() {
            this.entityAssertions.assertEntityMatchesJson(
                    this.handle,
                    this.jsonResourceName,
                    this.fieldsToIgnore.toArray(new String[0])
            );
        }

        /**
         * Compare the full JSON structure after removing ignored fields.
         */
        public void assertMatchesStrict() {
            this.entityAssertions.assertEntityMatchesJsonStrict(
                    this.handle,
                    this.jsonResourceName,
                    this.fieldsToIgnore.toArray(new String[0])
            );
        }
    }

    /**
     * Builder for matching multiple entities against JSON fixtures (order independent).
     */
    public static final class EntitiesAssertionBuilder<E> {
        private final DbContract<E> contract;
        private final Class<E> entityType;
        private final DbRetrieveFactory retrieveFactory;
        private final DbEntityAssertions entityAssertions;
        private final List<String> fieldsToIgnore;
        private Integer expectedSize;

        private EntitiesAssertionBuilder(final DbContract<E> contract,
                                         final Class<E> entityType,
                                         final DbRetrieveFactory retrieveFactory,
                                         final DbEntityAssertions entityAssertions) {
            this.contract = contract;
            this.entityType = entityType;
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

        /**
         * Assert the expected total number of entities for this contract.
         */
        public EntitiesAssertionBuilder<E> hasSize(final int expectedSize) {
            this.expectedSize = expectedSize;
            return this;
        }

        /**
         * Assert that every provided fixture matches a persisted entity (extra entities allowed).
         */
        public void containsAllWithJsons(final String... jsonResourceNames) {
            this.matchAll(jsonResourceNames);
        }

        /**
         * Same as {@link #containsAllWithJsons(String...)} but uses the default fixture path.
         */
        /**
         * Assert that the number of persisted entities equals the fixture count and that each
         * fixture matches a distinct entity.
         */
        public void containsExactlyWithJsons(final String... jsonResourceNames) {
            this.matchExactly(jsonResourceNames);
        }

        private void matchAll(final String[] jsonResourceNames) {
            if (jsonResourceNames == null) {
                throw new IllegalArgumentException("Json resource names must not be null");
            }
            final List<E> entities = this.retrieveFactory.forClass(this.entityType).getAll();
            this.assertExpectedSize(entities);
            final List<E> remaining = new ArrayList<>(entities);
            final String[] ignoreFields = this.fieldsToIgnore.toArray(new String[0]);

            for (final String jsonResourceName : jsonResourceNames) {
                boolean matched = false;
                AssertionError lastError = null;

                for (final Iterator<E> iterator = remaining.iterator(); iterator.hasNext(); ) {
                    final E entity = iterator.next();
                    final DbEntityHandle<E> handle = new DbEntityHandle<>(entity, this.contract);
                    try {
                        this.entityAssertions.assertEntityMatchesJson(handle, jsonResourceName, ignoreFields);
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
                                this.entityType.getSimpleName()), lastError);
                    }
                    throw new AssertionError(String.format("No entity matched json '%s' for contract %s",
                            jsonResourceName,
                            this.entityType.getSimpleName()));
                }
            }
        }

        private void matchExactly(final String[] jsonResourceNames) {
            if (jsonResourceNames == null) {
                throw new IllegalArgumentException("Json resource names must not be null");
            }

            final List<E> entities = this.retrieveFactory.forClass(this.entityType).getAll();
            this.assertExpectedSize(entities);
            if (entities.size() != jsonResourceNames.length) {
                throw new AssertionError(String.format("Entity count mismatch for contract %s: expected %d but was %d",
                        this.entityType.getSimpleName(),
                        jsonResourceNames.length,
                        entities.size()));
            }

            this.matchAll(jsonResourceNames);
        }

        private void assertExpectedSize(final List<E> entities) {
            if (this.expectedSize == null) {
                return;
            }
            if (entities.size() != this.expectedSize) {
                throw new AssertionError(String.format("Entity count mismatch for contract %s: expected %d but was %d",
                        this.entityType.getSimpleName(),
                        this.expectedSize,
                        entities.size()));
            }
        }
    }
}
