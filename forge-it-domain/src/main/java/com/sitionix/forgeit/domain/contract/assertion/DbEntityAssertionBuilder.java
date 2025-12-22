package com.sitionix.forgeit.domain.contract.assertion;

public interface DbEntityAssertionBuilder<E> {

    DbEntityAssertionBuilder<E> withJson(String jsonResourceName);

    DbEntityAssertionBuilder<E> ignoreFields(String... fields);

    DbEntityAssertionBuilder<E> withDeepStructure();

    DbEntityAssertionBuilder<E> withFetchedRelations();

    void assertMatches();

    void assertMatchesStrict();
}
