package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

public interface DbGraphResult {

    <E> E entity(DbContract<E> contract);
}
