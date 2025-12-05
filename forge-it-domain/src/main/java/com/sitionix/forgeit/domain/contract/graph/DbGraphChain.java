package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

public interface DbGraphChain<E> {

    E entity();

    <N> DbGraphChain<N> to(DbContract<N> nextContract);

    DbGraphResult build();
}
