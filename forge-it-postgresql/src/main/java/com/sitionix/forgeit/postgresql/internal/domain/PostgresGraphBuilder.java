package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbGraphBuilder;
import com.sitionix.forgeit.domain.contract.DbGraphChain;

public class PostgresGraphBuilder implements DbGraphBuilder {

    @Override
    public <E> DbGraphChain<E> to(final DbContract<E> contract) {
        return null;
    }
}
