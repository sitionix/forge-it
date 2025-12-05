package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.graph.DbGraphBuilder;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphContext;

public class PostgresGraphBuilder implements DbGraphBuilder {

    private final DbEntityFactory entityFactory;

    public PostgresGraphBuilder(final DbEntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public <E> DbGraphChain<E> to(final DbContract<E> contract) {
        if (contract == null) {
            throw new IllegalArgumentException("DbContract must not be null");
        }
        final DefaultDbGraphContext context = new DefaultDbGraphContext(this.entityFactory);
        return new PostgresDbGraphChain<>(context, contract);
    }
}
