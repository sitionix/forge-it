package com.sitionix.forgeit.domain.contract;

public interface DbEntityFactory {
    <E> E create(DbContractInvocation<E> invocation);
}
