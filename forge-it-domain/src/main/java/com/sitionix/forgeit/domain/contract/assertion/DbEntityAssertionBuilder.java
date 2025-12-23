package com.sitionix.forgeit.domain.contract.assertion;

import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;

public interface DbEntityAssertionBuilder<E> {

    DbEntityAssertionBuilder<E> withJson(String jsonResourceName);

    DbEntityAssertionBuilder<E> ignoreFields(String... fields);

    DbEntityAssertionBuilder<E> withDeepStructure();

    DbEntityAssertionBuilder<E> withFetchedRelations();

    DbEntityHandle<E> actualHandle();

    void assertMatches();

    void assertMatchesStrict();
}
