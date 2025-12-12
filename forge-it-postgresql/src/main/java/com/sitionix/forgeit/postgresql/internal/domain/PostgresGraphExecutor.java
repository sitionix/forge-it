package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostgresGraphExecutor {

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public DbGraphResult execute(final DbGraphContext context, final List<DbContractInvocation<?>> chain) {
        for (final DbContractInvocation<?> invocation : chain) {
            context.getOrCreate(invocation);
        }

        final Map<DbContract<?>, Object> original = context.snapshot();

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
    }
}
