package com.sitionix.forgeit.application.validator;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.assertion.DbEntitiesAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Builder for matching multiple entities against JSON fixtures (order independent).
 */
public final class EntitiesAssertionBuilder<E> implements DbEntitiesAssertionBuilder<E> {
    private final DbContract<E> contract;
    private final Class<E> entityType;
    private final DbEntityAssertions entityAssertions;
    private final DbEntityFetcher entityFetcher;
    private final List<String> fieldsToIgnore;
    private Integer expectedSize;
    private boolean deepStructure;

    public EntitiesAssertionBuilder(final DbContract<E> contract,
                                    final Class<E> entityType,
                                    final DbEntityAssertions entityAssertions,
                                    final DbEntityFetcher entityFetcher) {
        this.contract = contract;
        this.entityType = entityType;
        this.entityAssertions = entityAssertions;
        this.entityFetcher = entityFetcher;
        this.fieldsToIgnore = new ArrayList<>();
    }

    @Override
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
    @Override
    public EntitiesAssertionBuilder<E> hasSize(final int expectedSize) {
        this.expectedSize = expectedSize;
        return this;
    }

    /**
     * Reload entities before matching to ensure lazy associations are initialized.
     */
    @Override
    public EntitiesAssertionBuilder<E> withDeepStructure() {
        this.deepStructure = true;
        return this;
    }

    /**
     * Alias for {@link #withDeepStructure()}.
     */
    @Override
    public EntitiesAssertionBuilder<E> withFetchedRelations() {
        return this.withDeepStructure();
    }

    /**
     * Assert that every provided fixture matches a persisted entity (extra entities allowed).
     */
    @Override
    public void containsAllWithJsons(final String... jsonResourceNames) {
        this.matchAll(jsonResourceNames);
    }

    /**
     * Assert that the number of persisted entities equals the fixture count and that each
     * fixture matches a distinct entity.
     */
    @Override
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
