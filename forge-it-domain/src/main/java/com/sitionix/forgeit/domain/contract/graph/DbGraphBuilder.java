package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

public interface DbGraphBuilder {
    <E> DbGraphChain<E> to(DbContract<E> contract);

}
