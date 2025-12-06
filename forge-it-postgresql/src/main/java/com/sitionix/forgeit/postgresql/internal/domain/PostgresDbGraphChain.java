package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;

import java.util.ArrayList;
import java.util.List;

public final class PostgresDbGraphChain<E> implements DbGraphChain<E> {

    private final DbGraphContext context;
    private final List<DbContractInvocation<?>> chain;
    private final DbContractInvocation<E> last;

    public PostgresDbGraphChain(
            final DbGraphContext context,
            final DbContractInvocation<E> firstInvocation
    ) {
        this(context, List.of(firstInvocation), firstInvocation);
    }

    private PostgresDbGraphChain(
            final DbGraphContext context,
            final List<DbContractInvocation<?>> chain,
            final DbContractInvocation<E> last
    ) {
        this.context = context;
        this.chain = chain;
        this.last = last;
    }

    @Override
    public E entity() {
        return this.context.getOrCreate(this.last);
    }

    @Override
    public DbGraphResult build() {
        for (final DbContractInvocation<?> invocation : this.chain) {
            this.createIfNeeded(invocation);
        }
        return new DefaultDbGraphResult(this.context.snapshot());
    }

    @Override
    public <N> DbGraphChain<N> to(final DbContractInvocation<N> nextInvocation) {
        final List<DbContractInvocation<?>> nextChain = new ArrayList<>(this.chain);
        nextChain.add(nextInvocation);
        return new PostgresDbGraphChain<>(this.context, nextChain, nextInvocation);
    }

    private <N> void createIfNeeded(final DbContractInvocation<N> invocation) {
        this.context.getOrCreate(invocation);
    }
}

