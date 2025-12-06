package com.sitionix.forgeit.domain.contract;

import java.util.List;

public interface DbContract<E> {

    Class<E> entityType();

    List<DbDependency<E, ?>> dependencies();

    String defaultJsonResourceName();

    default DbContractInvocation<E> withJson(final String jsonResourceName) {
        final String effective = (jsonResourceName != null)
                ? jsonResourceName
                : this.defaultJsonResourceName();
        return new DbContractInvocation<>(this, effective);
    }
}


