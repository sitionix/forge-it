package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.ForgeItConfigurationException;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import com.sitionix.forgeit.postgresql.internal.config.PostgresqlProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostgresGraphExecutor {

    private final EntityManagerFactory emf;

    private final TransactionTemplate txTemplate;

    private final GraphTxPolicy txPolicy;

    public PostgresGraphExecutor(
            final EntityManagerFactory emf,
            final PlatformTransactionManager transactionManager,
            final PostgresqlProperties properties) {
        this.emf = emf;
        this.txPolicy = properties.getTxPolicy() == null
                ? GraphTxPolicy.REQUIRES_NEW
                : properties.getTxPolicy();
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setPropagationBehavior(this.txPolicy.propagationBehavior());
    }

    public DbGraphResult execute(final DbGraphContext context, final List<DbContractInvocation<?>> chain) {
        this.guardMandatoryTransaction();

        return this.txTemplate.execute(status -> {
            final EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(this.emf);
            if (em == null) {
                throw new IllegalStateException("No transactional EntityManager bound to current thread");
            }

            for (final DbContractInvocation<?> invocation : chain) {
                context.getOrCreate(invocation);
            }

            final Map<DbContract<?>, Object> original = context.snapshot();
            final Map<DbContract<?>, Object> managedMap = new LinkedHashMap<>();

            for (final var entry : original.entrySet()) {
                final Object entity = entry.getValue();
                if (entity == null) {
                    managedMap.put(entry.getKey(), null);
                    continue;
                }
                final Object managed = em.contains(entity) ? entity : em.merge(entity);
                managedMap.put(entry.getKey(), managed);
            }

            em.flush();
            return new DefaultDbGraphResult(Map.copyOf(managedMap));
        });
    }

    private void guardMandatoryTransaction() {
        if (this.txPolicy == GraphTxPolicy.MANDATORY
                && !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new ForgeItConfigurationException("GraphTxPolicy.MANDATORY requires an active transaction. "
                    + "Switch to REQUIRED/REQUIRES_NEW or annotate the caller with @Transactional.");
        }
    }
}

