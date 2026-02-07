package com.sitionix.forgeit.domain.model.sql;

import java.util.List;
import java.util.function.Predicate;

public interface DbRetriever<E> {

    E getById(Object id);

    List<E> getAll();

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
