package com.sitionix.forgeit.domain.contract.assertion;

import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;

import java.util.List;

public interface DbEntitiesAssertionBuilder<E> {

    DbEntitiesAssertionBuilder<E> ignoreFields(String... fields);

    DbEntitiesAssertionBuilder<E> hasSize(int expectedSize);

    DbEntitiesAssertionBuilder<E> withDeepStructure();

    DbEntitiesAssertionBuilder<E> withFetchedRelations();

    List<DbEntityHandle<E>> actualHandles();

    void containsAllWithJsons(String... jsonResourceNames);

    void containsWithJsonsStrict(String... jsonResourceNames);
}
