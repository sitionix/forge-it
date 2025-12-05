package com.sitionix.forgeit.domain.contract;

import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;

@RequiredArgsConstructor
public final class DbDependency<C, P> {
    private final DbContract<P> parent;
    private final BiConsumer<C, P> attach;

    public DbContract<P> parent() {
        return this.parent;
    }

    public BiConsumer<C, P> attach() {
        return this.attach;
    }
}
