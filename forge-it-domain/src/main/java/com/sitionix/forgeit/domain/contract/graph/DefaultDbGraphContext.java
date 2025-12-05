package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbDependency;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class DefaultDbGraphContext implements DbGraphContext {

    private final DbEntityFactory entityFactory;
    private final Map<DbContract<?>, Object> cache = new LinkedHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <E> E getOrCreate(final DbContract<E> contract) {
        return (E) this.cache.computeIfAbsent(contract, c -> {
            final DbContract<E> typed = (DbContract<E>) c;

            final E entity = this.entityFactory.create(typed);

            final List<DbDependency<E, ?>> dependencies = typed.dependencies();
            if (dependencies != null && !dependencies.isEmpty()) {
                for (final DbDependency<E, ?> dep : dependencies) {
                    this.attachDependency(entity, dep);
                }
            }

            return entity;
        });
    }

    private <E, P> void attachDependency(final E child, final DbDependency<E, P> dep) {
        final P parent = this.getOrCreate(dep.parent());
        dep.attach().accept(child, parent);
    }

    @Override
    public Map<DbContract<?>, Object> snapshot() {
        return Map.copyOf(this.cache);
    }
}
