package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphChain;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import jakarta.persistence.EntityManager;

import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PostgresDbGraphChain<E> implements DbGraphChain<E> {

    private final DbGraphContext context;
    private final List<DbContractInvocation<?>> chain;
    private final DbContractInvocation<E> last;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public PostgresDbGraphChain(
            final DbGraphContext context,
            final DbContractInvocation<E> firstInvocation,
            final EntityManager entityManager,
            final TransactionTemplate transactionTemplate
    ) {
        this(context,
                List.of(firstInvocation),
                firstInvocation,
                entityManager,
                transactionTemplate);
    }

    private PostgresDbGraphChain(
            final DbGraphContext context,
            final List<DbContractInvocation<?>> chain,
            final DbContractInvocation<E> last,
            final EntityManager entityManager,
            final TransactionTemplate transactionTemplate
            ) {
        this.context = context;
        this.chain = chain;
        this.last = last;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public E entity() {
        return this.context.getOrCreate(this.last);
    }

    @Override
    public DbGraphResult build() {
        return this.transactionTemplate.execute(status -> {
            for (final DbContractInvocation<?> invocation : this.chain) {
                this.context.getOrCreate(invocation);
            }
            final Map<DbContract<?>, Object> original = this.context.snapshot();

            final Map<DbContract<?>, Object> managedMap = new LinkedHashMap<>();

            for (final Map.Entry<DbContract<?>, Object> entry : original.entrySet()) {
                final DbContract<?> contract = entry.getKey();
                final Object entity = entry.getValue();

                if (entity == null) {
                    managedMap.put(contract, null);
                    continue;
                }

                final Object managedEntity;
                if (this.entityManager.contains(entity)) {
                    managedEntity = entity;
                } else {
                    managedEntity = this.entityManager.merge(entity);
                }

                managedMap.put(contract, managedEntity);
            }

            this.entityManager.flush();

            return new DefaultDbGraphResult(Map.copyOf(managedMap));
        });
    }

    @Override
    public <N> DbGraphChain<N> to(final DbContractInvocation<N> nextInvocation) {
        final List<DbContractInvocation<?>> nextChain = new ArrayList<>(this.chain);
        nextChain.add(nextInvocation);
        return new PostgresDbGraphChain<>(this.context,
                nextChain,
                nextInvocation,
                this.entityManager,
                this.transactionTemplate);
    }
}

