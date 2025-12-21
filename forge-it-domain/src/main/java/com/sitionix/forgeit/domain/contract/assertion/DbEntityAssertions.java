package com.sitionix.forgeit.domain.contract.assertion;

import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;

public interface DbEntityAssertions {

    <E> void assertEntityMatchesJson(DbEntityHandle<E> handle,
                                     String jsonResourceName,
                                     String... fieldsToIgnore);

    <E> void assertEntityMatchesDefaultJson(DbEntityHandle<E> handle,
                                            String jsonResourceName,
                                            String... fieldsToIgnore);

    <E> void assertEntityMatchesJsonStrict(DbEntityHandle<E> handle,
                                           String jsonResourceName,
                                           String... fieldsToIgnore);

    <E> void assertEntityMatchesDefaultJsonStrict(DbEntityHandle<E> handle,
                                                  String jsonResourceName,
                                                  String... fieldsToIgnore);
}
