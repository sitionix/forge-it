package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;

import java.util.ArrayList;
import java.util.List;

public class PostgresDbGraphChain<E> implements DbGraphChain<E> {

    private final DbGraphContext context;
    private final List<DbContract<?>> chain;
    private final DbContract<E> last;

    public PostgresDbGraphChain(final DbGraphContext context,
                                final DbContract<E> firstContract) {
        this(context, List.of(firstContract), firstContract);
    }

    private PostgresDbGraphChain(final DbGraphContext context,
                                 final List<DbContract<?>> chain,
                                 final DbContract<E> last) {
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
        for (final DbContract<?> contract : this.chain) {
            this.createIfNeeded(contract);
        }
        return new DefaultDbGraphResult(this.context.snapshot());
    }

    @Override
    public <N> DbGraphChain<N> to(final DbContract<N> nextContract) {
        final List<DbContract<?>> nextChain = new ArrayList<>(this.chain);
        nextChain.add(nextContract);
        return new PostgresDbGraphChain<>(this.context, nextChain, nextContract);
    }

    private <N> void createIfNeeded(final DbContract<N> contract) {
        this.context.getOrCreate(contract);
    }
}
