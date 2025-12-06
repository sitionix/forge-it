package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphContext;

public final class PostgresGraphBuilder implements DbGraphBuilder {

    private final DbEntityFactory entityFactory;

    public PostgresGraphBuilder(final DbEntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public <E> DbGraphChain<E> to(final DbContractInvocation<E> invocation) {
        if (invocation == null) {
            throw new IllegalArgumentException("DbContractInvocation must not be null");
        }

        final DbGraphContext context = new DefaultDbGraphContext(this.entityFactory);
        return new PostgresDbGraphChain<>(context, invocation);
    }
}

