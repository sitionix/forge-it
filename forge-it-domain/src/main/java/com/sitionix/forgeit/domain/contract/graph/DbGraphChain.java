package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;

public interface DbGraphChain<E> {

    E entity();

    <N> DbGraphChain<N> to(DbContractInvocation<N> nextContract);

    default <N> DbGraphChain<N> to(final DbContract<N> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        return this.to(contract.withJson(null));
    }

    DbGraphResult build();
}
