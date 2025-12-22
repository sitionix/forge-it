package com.sitionix.forgeit.postgresql.internal.repository;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;
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

    private final DbEntityFetcher entityFetcher;

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
        return new EntityAssertionBuilder<>(handle, this.entityAssertions, this.entityFetcher);
    }

    /**
     * Start an assertion workflow for all entities stored for the given contract.
     */
    public <E> EntitiesAssertionBuilder<E> assertEntities(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return new EntitiesAssertionBuilder<>(contract,
                contract.entityType(),
                this.entityAssertions,
                this.entityFetcher);
    }

    /**
     * Start an assertion workflow for all entities of the given type.
     */
    public <E> EntitiesAssertionBuilder<E> assertEntities(final Class<E> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type must not be null");
        }
        return new EntitiesAssertionBuilder<>(null,
                entityType,
                this.entityAssertions,
                this.entityFetcher);
    }

    /**
     * Builder for asserting a single entity against a JSON fixture.
     */
    public static final class EntityAssertionBuilder<E> {
        private final DbEntityHandle<E> handle;
        private final DbEntityAssertions entityAssertions;
        private final DbEntityFetcher entityFetcher;
        private final List<String> fieldsToIgnore;
        private String jsonResourceName;
        private boolean deepStructure;

        private EntityAssertionBuilder(final DbEntityHandle<E> handle,
                                       final DbEntityAssertions entityAssertions,
                                       final DbEntityFetcher entityFetcher) {
            this.handle = handle;
            this.entityAssertions = entityAssertions;
            this.entityFetcher = entityFetcher;
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
         * Reload the entity from the database by id before comparison.
         */
        public EntityAssertionBuilder<E> withDeepStructure() {
            this.deepStructure = true;
            return this;
        }

        /**
         * Alias for {@link #withDeepStructure()}.
         */
        public EntityAssertionBuilder<E> withFetchedRelations() {
            return this.withDeepStructure();
        }

        /**
         * Compare only the fields present in the JSON fixture.
         */
        public void assertMatches() {
            final DbEntityHandle<E> effectiveHandle = this.resolveHandle();
            this.entityAssertions.assertEntityMatchesJson(
                    effectiveHandle,
                    this.jsonResourceName,
                    this.fieldsToIgnore.toArray(new String[0])
            );
        }

        /**
         * Compare the full JSON structure after removing ignored fields.
         */
        public void assertMatchesStrict() {
            final DbEntityHandle<E> effectiveHandle = this.resolveHandle();
            this.entityAssertions.assertEntityMatchesJsonStrict(
                    effectiveHandle,
                    this.jsonResourceName,
                    this.fieldsToIgnore.toArray(new String[0])
            );
        }

        private DbEntityHandle<E> resolveHandle() {
            if (!this.deepStructure) {
                return this.handle;
            }
            if (this.handle.contract() == null) {
                throw new IllegalStateException("Deep structure requires a contract-backed handle");
            }
            final Object id = new org.springframework.beans.BeanWrapperImpl(this.handle.get())
                    .getPropertyValue("id");
            if (id == null) {
                throw new IllegalStateException("Deep structure requires a non-null id");
            }
            final E reloaded = this.entityFetcher.reloadByIdWithRelations(this.handle.contract(), id);
            if (reloaded == null) {
                throw new IllegalStateException("Deep structure reload returned null");
            }
            return new DbEntityHandle<>(reloaded, this.handle.contract());
        }

    }

    /**
     * Builder for matching multiple entities against JSON fixtures (order independent).
     */
    public static final class EntitiesAssertionBuilder<E> {
        private final DbContract<E> contract;
        private final Class<E> entityType;
        private final DbEntityAssertions entityAssertions;
        private final DbEntityFetcher entityFetcher;
        private final List<String> fieldsToIgnore;
        private Integer expectedSize;
        private boolean deepStructure;

        private EntitiesAssertionBuilder(final DbContract<E> contract,
                                         final Class<E> entityType,
                                         final DbEntityAssertions entityAssertions,
                                         final DbEntityFetcher entityFetcher) {
            this.contract = contract;
            this.entityType = entityType;
            this.entityAssertions = entityAssertions;
            this.entityFetcher = entityFetcher;
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
         * Reload entities before matching to ensure lazy associations are initialized.
         */
        public EntitiesAssertionBuilder<E> withDeepStructure() {
            this.deepStructure = true;
            return this;
        }

        /**
         * Alias for {@link #withDeepStructure()}.
         */
        public EntitiesAssertionBuilder<E> withFetchedRelations() {
            return this.withDeepStructure();
        }

        /**
         * Assert that every provided fixture matches a persisted entity (extra entities allowed).
         */
        public void containsAllWithJsons(final String... jsonResourceNames) {
            this.matchAll(jsonResourceNames);
        }

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
            final List<E> entities = this.loadEntities();
            if (entities.isEmpty()) {
                throw new AssertionError(String.format("No entities found for contract %s",
                        this.entityType.getSimpleName()));
            }
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
                    final String baseMessage = String.format(
                            "No entity matched json '%s' for contract %s (scanned %d entities)",
                            jsonResourceName,
                            this.entityType.getSimpleName(),
                            entities.size());
                    if (lastError != null) {
                        throw new AssertionError(baseMessage + ": " + lastError.getMessage());
                    }
                    throw new AssertionError(baseMessage);
                }
            }
        }

        private void matchExactly(final String[] jsonResourceNames) {
            if (jsonResourceNames == null) {
                throw new IllegalArgumentException("Json resource names must not be null");
            }

            final List<E> entities = this.loadEntities();
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

        private List<E> loadEntities() {
            if (!this.deepStructure) {
                return this.entityFetcher.loadAll(this.entityType);
            }
            return this.entityFetcher.loadAllWithRelations(this.entityType);
        }
    }
}
