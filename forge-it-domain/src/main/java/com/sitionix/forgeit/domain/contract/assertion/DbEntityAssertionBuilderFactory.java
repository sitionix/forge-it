package com.sitionix.forgeit.domain.contract.assertion;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;

public interface DbEntityAssertionBuilderFactory {

    <E> DbEntityAssertionBuilder<E> forEntity(DbEntityHandle<E> handle);

    <E> DbEntitiesAssertionBuilder<E> forContract(DbContract<E> contract);

    <E> DbEntitiesAssertionBuilder<E> forEntityType(Class<E> entityType);
}
