package com.sitionix.forgeit.domain.model.sql;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface DbRetriever<E> {

    E getById(Object id);

    List<E> getAll();

    default <V> DbRetriever<E> where(final Function<E, V> extractor, final V expectedValue) {
        Objects.requireNonNull(extractor, "Field extractor must not be null");
        return new DbFilteredRetriever<>(this, entity -> Objects.equals(extractor.apply(entity), expectedValue));
    }

    default DbRetriever<E> where(final Predicate<E> predicate) {
        return new DbFilteredRetriever<>(this,
                Objects.requireNonNull(predicate, "Expected predicate must not be null"));
    }

    default DbEntitiesAssertion<E> hasSize(final int expectedSize) {
        return DbEntitiesAssertionChain.<E>forRetriever(this)
                .hasSize(expectedSize);
    }

    default DbEntitiesAssertion<E> andExpected(final Predicate<E> predicate) {
        return DbEntitiesAssertionChain.<E>forRetriever(this)
                .andExpected(predicate);
    }

    default DbSingleEntityAssertion<E> singleElement() {
        return DbEntitiesAssertionChain.<E>forRetriever(this)
                .singleElement();
    }

    default void allMatch() {
        DbEntitiesAssertionChain.<E>forRetriever(this)
                .allMatch();
    }

    default void anyMatch() {
        DbEntitiesAssertionChain.<E>forRetriever(this)
                .anyMatch();
    }

    default void nonMatch() {
        DbEntitiesAssertionChain.<E>forRetriever(this)
                .nonMatch();
    }
}
