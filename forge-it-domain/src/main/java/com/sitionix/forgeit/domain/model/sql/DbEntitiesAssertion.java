package com.sitionix.forgeit.domain.model.sql;

import java.util.function.Predicate;

public interface DbEntitiesAssertion<E> {

    DbEntitiesAssertion<E> hasSize(int expectedSize);

    DbEntitiesAssertion<E> andExpected(Predicate<E> predicate);

    DbSingleEntityAssertion<E> singleElement();

    void allMatch();

    void anyMatch();

    void nonMatch();
}
