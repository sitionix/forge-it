package com.sitionix.forgeit.domain.contract;

import java.util.List;

public interface DbContract<E> {

    Class<E> entityType();

    List<DbDependency<E, ?>> dependencies();

    default DbContractInvocation<E> withJson(final String jsonResourceName) {
        return new DbContractInvocation<>(this, jsonResourceName);
    }
}


