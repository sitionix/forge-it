package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbDependency;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class DefaultDbGraphContext implements DbGraphContext {

    private final DbEntityFactory entityFactory;
    private final Map<DbContractInvocation<?>, Object> invocationCache = new LinkedHashMap<>();
    private final Map<DbContract<?>, Object> contractCache = new LinkedHashMap<>();


    @Override
    @SuppressWarnings("unchecked")
    public synchronized <E> E getOrCreate(final DbContractInvocation<E> invocation) {
        final DbContract<E> contract = invocation.getContract();

        return (E) this.invocationCache.computeIfAbsent(invocation, c -> {
            final E entity = this.entityFactory.create(invocation);

            final List<DbDependency<E, ?>> dependencies = contract.dependencies();
            if (dependencies != null && !dependencies.isEmpty()) {
                for (final DbDependency<E, ?> dependency : dependencies) {
                    this.attachDependency(entity, dependency);
                }
            }

            this.contractCache.put(contract, entity);
            return entity;
        });
    }

    private <E, P> void attachDependency(final E child,
            final DbDependency<E, P> dependency) {

        final DbContract<P> parentContract = dependency.parent();
        @SuppressWarnings("unchecked")
        final P parent = (P) this.contractCache.get(parentContract);
        if (parent == null) {
            final DbContractInvocation<P> parentInvocation =
                    new DbContractInvocation<>(parentContract, null);
            final P created = this.getOrCreate(parentInvocation);
            dependency.attach().accept(child, created);
            return;
        }
        dependency.attach().accept(child, parent);
    }

    @Override
    public Map<DbContractInvocation<?>, Object> snapshot() {
        return new LinkedHashMap<>(this.invocationCache);
    }
}
