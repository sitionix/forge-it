package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;

import java.util.ArrayList;
import java.util.List;

public final class PostgresDbGraphChain<E> implements DbGraphChain<E> {

    private final DbGraphContext context;
    private final List<DbContractInvocation<?>> chain;
    private final DbContractInvocation<E> last;
    private final PostgresGraphExecutor graphExecutor;

    public PostgresDbGraphChain(
            final DbGraphContext context,
            final DbContractInvocation<E> firstInvocation,
            final PostgresGraphExecutor graphExecutor) {
        this(context,
                List.of(firstInvocation),
                firstInvocation,
                graphExecutor);
    }

    private PostgresDbGraphChain(
            final DbGraphContext context,
            final List<DbContractInvocation<?>> chain,
            final DbContractInvocation<E> last,
            final PostgresGraphExecutor graphExecutor) {
        this.context = context;
        this.chain = chain;
        this.last = last;
        this.graphExecutor = graphExecutor;
    }

    @Override
    public E entity() {
        return this.context.getOrCreate(this.last);
    }

    @Override
    public DbGraphResult build() {
            return this.graphExecutor.execute(this.context, this.chain);
    }

    @Override
    public <N> DbGraphChain<N> to(final DbContractInvocation<N> nextInvocation) {
        final List<DbContractInvocation<?>> nextChain = new ArrayList<>(this.chain);
        this.appendInvocation(nextChain, nextInvocation);
        return new PostgresDbGraphChain<>(this.context,
                nextChain,
                nextInvocation,
                this.graphExecutor);
    }

    private void appendInvocation(final List<DbContractInvocation<?>> chain,
                                  final DbContractInvocation<?> invocation) {
        final List<DbContractInvocation<?>> before = new ArrayList<>();
        final List<DbContractInvocation<?>> after = new ArrayList<>();

        for (final DbContractInvocation<?> child : invocation.getChildren()) {
            if (this.dependsOn(invocation.getContract(), child.getContract())) {
                before.add(child);
            } else if (this.dependsOn(child.getContract(), invocation.getContract())) {
                after.add(child);
            } else {
                before.add(child);
            }
        }

        for (final DbContractInvocation<?> child : before) {
            this.appendInvocation(chain, child);
        }

        chain.add(invocation);

        for (final DbContractInvocation<?> child : after) {
            this.appendInvocation(chain, child);
        }
    }

    private boolean dependsOn(final DbContract<?> contract,
                              final DbContract<?> parent) {
        if (contract.dependencies() == null) {
            return false;
        }
        return contract.dependencies()
                .stream()
                .anyMatch(dep -> dep.parent().equals(parent));
    }
}
