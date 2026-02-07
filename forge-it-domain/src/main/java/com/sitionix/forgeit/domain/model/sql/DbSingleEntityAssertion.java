package com.sitionix.forgeit.domain.model.sql;

import java.util.function.Predicate;

public interface DbSingleEntityAssertion<E> {

    DbSingleEntityAssertion<E> andExpected(Predicate<E> predicate);

    E assertEntity();
}
