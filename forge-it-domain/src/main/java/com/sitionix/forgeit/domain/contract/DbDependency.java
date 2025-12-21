package com.sitionix.forgeit.domain.contract;

import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;

public final class DbDependency<C, P> {
    private final DbContract<P> parent;
    private final BiConsumer<C, P> attach;
    private final boolean optional;

    public DbDependency(final DbContract<P> parent, final BiConsumer<C, P> attach) {
        this(parent, attach, false);
    }

    public DbDependency(final DbContract<P> parent,
                        final BiConsumer<C, P> attach,
                        final boolean optional) {
        this.parent = parent;
        this.attach = attach;
        this.optional = optional;
    }

    public DbContract<P> parent() {
        return this.parent;
    }

    public BiConsumer<C, P> attach() {
        return this.attach;
    }

    public boolean optional() {
        return this.optional;
    }
}
