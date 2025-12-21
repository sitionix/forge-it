package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;

import java.util.Map;

public interface DbGraphContext {
    <E> E getOrCreate(DbContractInvocation<E> contract);

    Map<DbContractInvocation<?>, Object> snapshot();
}
