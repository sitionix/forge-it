package com.sitionix.forgeit.domain.model.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class DbEntitiesAssertionChain<E> implements DbEntitiesAssertion<E> {

    private final DbRetriever<E> retriever;
    private final List<Predicate<E>> expectations;
    private Integer expectedSize;
    private List<E> cachedEntities;

    private DbEntitiesAssertionChain(final DbRetriever<E> retriever) {
        this.retriever = Objects.requireNonNull(retriever, "DbRetriever must not be null");
        this.expectations = new ArrayList<>();
    }

    public static <E> DbEntitiesAssertionChain<E> forRetriever(final DbRetriever<E> retriever) {
        return new DbEntitiesAssertionChain<>(retriever);
    }

    @Override
    public DbEntitiesAssertion<E> hasSize(final int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("Expected size must not be negative");
        }
        this.expectedSize = expectedSize;
        final List<E> entities = this.loadEntities();
        this.assertExpectedSize(entities);
        return this;
    }

    @Override
    public DbEntitiesAssertion<E> andExpected(final Predicate<E> predicate) {
        this.expectations.add(Objects.requireNonNull(predicate, "Expected predicate must not be null"));
        return this;
    }

    @Override
    public DbSingleEntityAssertion<E> singleElement() {
        if (this.expectedSize != null && this.expectedSize != 1) {
            throw new IllegalStateException("singleElement() can be used only when expected size is 1");
        }
        this.expectedSize = 1;
        final List<E> entities = this.loadEntities();
        this.assertExpectedSize(entities);
        return new SingleEntityAssertionChain<>(this);
    }

    @Override
    public void allMatch() {
        this.assertExpectationsConfigured();
        final List<E> entities = this.loadEntities();
        this.assertExpectedSize(entities);
        if (entities.isEmpty()) {
            throw new AssertionError("No entities found for assertion");
        }
        int index = 0;
        for (final E entity : entities) {
            if (!this.matches(entity)) {
                throw new AssertionError(String.format("Entity at index %d did not match expected predicates", index));
            }
            index++;
        }
    }

    @Override
    public void anyMatch() {
        this.assertExpectationsConfigured();
        final List<E> entities = this.loadEntities();
        this.assertExpectedSize(entities);
        if (entities.isEmpty()) {
            throw new AssertionError("No entities found for assertion");
        }
        for (final E entity : entities) {
            if (this.matches(entity)) {
                return;
            }
        }
        throw new AssertionError("No entity matched expected predicates");
    }

    @Override
    public void nonMatch() {
        this.assertExpectationsConfigured();
        final List<E> entities = this.loadEntities();
        this.assertExpectedSize(entities);
        for (final E entity : entities) {
            if (this.matches(entity)) {
                throw new AssertionError("Found entity matching expected predicates");
            }
        }
    }

    private List<E> loadEntities() {
        if (this.cachedEntities != null) {
            return this.cachedEntities;
        }
        final List<E> loaded = this.retriever.getAll();
        this.cachedEntities = loaded == null ? List.of() : loaded;
        return this.cachedEntities;
    }

    private void assertExpectedSize(final List<E> entities) {
        if (this.expectedSize == null) {
            return;
        }
        if (entities.size() != this.expectedSize) {
            throw new AssertionError(String.format(
                    "Entity count mismatch: expected %d but was %d",
                    this.expectedSize,
                    entities.size()
            ));
        }
    }

    private void assertExpectationsConfigured() {
        if (this.expectations.isEmpty()) {
            throw new IllegalStateException("No expectations configured. Call andExpected(...) before match assertions.");
        }
    }

    private boolean matches(final E entity) {
        for (final Predicate<E> predicate : this.expectations) {
            if (!predicate.test(entity)) {
                return false;
            }
        }
        return true;
    }

    private static final class SingleEntityAssertionChain<E> implements DbSingleEntityAssertion<E> {

        private final DbEntitiesAssertionChain<E> parent;

        private SingleEntityAssertionChain(final DbEntitiesAssertionChain<E> parent) {
            this.parent = parent;
        }

        @Override
        public DbSingleEntityAssertion<E> andExpected(final Predicate<E> predicate) {
            this.parent.andExpected(predicate);
            return this;
        }

        @Override
        public E assertEntity() {
            final List<E> entities = this.parent.loadEntities();
            this.parent.assertExpectedSize(entities);
            if (entities.size() != 1) {
                throw new AssertionError(String.format("Expected single entity but found %d", entities.size()));
            }
            final E entity = entities.get(0);
            if (!this.parent.expectations.isEmpty() && !this.parent.matches(entity)) {
                throw new AssertionError("Single entity did not match expected predicates");
            }
            return entity;
        }
    }
}
