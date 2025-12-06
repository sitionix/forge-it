package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;

public interface DbGraphBuilder {
    <E> DbGraphChain<E> to(DbContractInvocation<E> contract);

    default <E> DbGraphChain<E> to(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return this.to(contract.withJson(null));
    }
}
