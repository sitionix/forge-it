package com.sitionix.forgeit.sqlite.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphContext;

public final class SqliteGraphBuilder implements DbGraphBuilder {

    private final DbEntityFactory entityFactory;
    private final SqliteGraphExecutor graphExecutor;

    public SqliteGraphBuilder(final DbEntityFactory entityFactory,
                              final SqliteGraphExecutor graphExecutor) {
        this.entityFactory = entityFactory;
        this.graphExecutor = graphExecutor;
    }

    @Override
    public <E> DbGraphChain<E> to(final DbContractInvocation<E> root) {
        final var context = new DefaultDbGraphContext(this.entityFactory);
        return new SqliteDbGraphChain<>(context,
                root,
                this.graphExecutor);
    }
}
