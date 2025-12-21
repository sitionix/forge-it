package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public final class DefaultDbGraphResult implements DbGraphResult {

    private final Map<DbContract<?>, Object> entities;
    private final Map<DbContract<?>, Map<String, Object>> labeledEntities;

    @Override
    @SuppressWarnings("unchecked")
    public <E> DbEntityHandle<E> entity(final DbContract<E> contract) {
        return new DbEntityHandle<>((E) this.entities.get(contract));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> DbEntityHandle<E> entity(final DbContract<E> contract, final String label) {
        final Map<String, Object> labeled = this.labeledEntities.get(contract);
        if (labeled == null) {
            return new DbEntityHandle<>(null);
        }
        return new DbEntityHandle<>((E) labeled.get(label));
    }
}
