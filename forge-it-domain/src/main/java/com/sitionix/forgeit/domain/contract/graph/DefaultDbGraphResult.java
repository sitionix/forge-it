package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class DefaultDbGraphResult implements DbGraphResult {

    private final Map<DbContract<?>, Object> entities;
    private final Map<DbContract<?>, Map<String, Object>> labeledEntities;
    private final Map<DbContract<?>, List<Object>> orderedEntities;

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

    @Override
    @SuppressWarnings("unchecked")
    public <E> List<E> entities(final DbContract<E> contract) {
        return (List<E>) this.orderedEntities.getOrDefault(contract, Collections.emptyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> DbEntityHandle<E> entityAt(final DbContract<E> contract, final int index) {
        final List<Object> entries = this.orderedEntities.get(contract);
        if (entries == null || index < 0 || index >= entries.size()) {
            return new DbEntityHandle<>(null);
        }
        return new DbEntityHandle<>((E) entries.get(index));
    }
}
