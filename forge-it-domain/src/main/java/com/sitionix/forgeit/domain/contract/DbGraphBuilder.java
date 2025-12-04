package com.sitionix.forgeit.domain.contract;

public interface DbGraphBuilder {
    <E> DbGraphChain<E> to(DbContract<E> contract);

}
