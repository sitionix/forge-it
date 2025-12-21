package com.sitionix.forgeit.domain.contract.assertion;

import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;

public interface DbEntityAssertions {

    /**
     * Assert that the entity matches the provided JSON fixture by comparing only the fields
     * present in the fixture (extra entity fields are ignored). Optional fields can be ignored
     * explicitly.
     */
    <E> void assertEntityMatchesJson(DbEntityHandle<E> handle,
                                     String jsonResourceName,
                                     String... fieldsToIgnore);

    /**
     * Assert a strict JSON match between the entity and fixture (after removing ignored fields).
     */
    <E> void assertEntityMatchesJsonStrict(DbEntityHandle<E> handle,
                                           String jsonResourceName,
                                           String... fieldsToIgnore);

}
