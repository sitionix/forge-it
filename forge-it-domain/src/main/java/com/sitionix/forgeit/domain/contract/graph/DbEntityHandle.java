package com.sitionix.forgeit.domain.contract.graph;

import java.util.function.Consumer;

public final class DbEntityHandle<E> {

    private final E entity;

    public DbEntityHandle(final E entity) {
        this.entity = entity;
    }

    public E get() {
        return this.entity;
    }

    public DbEntityHandle<E> update(final Consumer<E> updater) {
        if (this.entity != null) {
            updater.accept(this.entity);
        }
        return this;
    }
}
