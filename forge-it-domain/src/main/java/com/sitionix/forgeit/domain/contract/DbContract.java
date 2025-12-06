package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.JsonBodySpec;

import java.util.List;

public interface DbContract<E> {

    Class<E> entityType();

    List<DbDependency<E, ?>> dependencies();

    String defaultJsonResourceName();

    default DbContractInvocation<E> withJson(final String jsonResourceName) {
        if (jsonResourceName != null) {
            return new DbContractInvocation<>(this, JsonBodySpec.explicitBody(jsonResourceName));
        }
        return new DbContractInvocation<>(this, JsonBodySpec.defaultBody(this.defaultJsonResourceName()));
    }
}


