package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.function.Consumer;

public final class DbEntityHandle<E> {

    private final E entity;
    private final DbContract<E> contract;

    public DbEntityHandle(final E entity, final DbContract<E> contract) {
        this.entity = entity;
        this.contract = contract;
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
        }
        return this;
    }
}
