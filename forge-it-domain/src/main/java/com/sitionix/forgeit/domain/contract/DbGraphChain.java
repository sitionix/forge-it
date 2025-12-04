package com.sitionix.forgeit.domain.contract;

public interface DbGraphChain<E> {

    E entity();

    <N> DbGraphChain<N> to(DbContract<N> nextContract);

    DbGraphResult build();
}
