package com.sitionix.forgeit.domain.contract.assertion;

public interface DbEntitiesAssertionBuilder<E> {

    DbEntitiesAssertionBuilder<E> ignoreFields(String... fields);

    DbEntitiesAssertionBuilder<E> hasSize(int expectedSize);

    DbEntitiesAssertionBuilder<E> withDeepStructure();

    DbEntitiesAssertionBuilder<E> withFetchedRelations();

    void containsAllWithJsons(String... jsonResourceNames);

    void containsExactlyWithJsons(String... jsonResourceNames);
}
