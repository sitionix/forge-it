package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public final class DefaultDbGraphResult implements DbGraphResult {

    private final Map<DbContract<?>, Object> entities;

    @Override
    @SuppressWarnings("unchecked")
    public <E> E entity(final DbContract<E> contract) {
        return (E) this.entities.get(contract);
    }
}
