package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.core.diagnostics.ForgeItTxDiagnostics;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PostgresGraphExecutor {

    private final EntityManagerFactory emf;
    private final ApplicationContext applicationContext;

    public PostgresGraphExecutor(final EntityManagerFactory emf, final ApplicationContext applicationContext) {
        this.emf = emf;
        this.applicationContext = applicationContext;
    }

    public DbGraphResult execute(final DbGraphContext context, final List<DbContractInvocation<?>> chain) {
        final EntityManager em = ForgeItTxDiagnostics.requireTransactionalEntityManager(
                this.applicationContext,
                this.emf,
                "PostgresGraphExecutor.execute"
        );

        return this.executeGraph(em, context, chain);
    }

    private DbGraphResult executeGraph(
            final EntityManager em,
            final DbGraphContext context,
            final List<DbContractInvocation<?>> chain
    ) {
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

            final Object managedEntity = em.contains(entity) ? entity : em.merge(entity);
            managedMap.put(contract, managedEntity);
        }

        em.flush();
        return new DefaultDbGraphResult(Map.copyOf(managedMap));
    }
}
