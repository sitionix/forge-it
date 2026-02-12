package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.function.Consumer;

public final class DbEntityHandle<E> {

    private final E entity;
    private final DbContract<E> contract;
    private final Consumer<E> persistAction;

    public DbEntityHandle(final E entity, final DbContract<E> contract) {
        this(entity, contract, null);
    }

    public DbEntityHandle(final E entity,
                          final DbContract<E> contract,
                          final Consumer<E> persistAction) {
        this.entity = entity;
        this.contract = contract;
        this.persistAction = persistAction;
    }

    public E get() {
        return this.entity;
    }

    /**
     * Contract that produced this entity handle, when available.
     */
    public DbContract<E> contract() {
        return this.contract;
    }

    public DbEntityHandle<E> update(final Consumer<E> updater) {
        if (this.entity != null) {
            updater.accept(this.entity);
            if (this.persistAction != null) {
                this.persistAction.accept(this.entity);
            }
        }
        return this;
    }

    public DbEntityHandle<E> mutate(final Consumer<E> updater) {
        return this.update(updater);
    }
}
