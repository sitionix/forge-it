package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.Map;

public interface DbGraphContext {
    <E> E getOrCreate(DbContract<E> contract);

    Map<DbContract<?>, Object> snapshot();
}
