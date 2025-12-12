package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.application.executor.TestRollbackContextHolder;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import jakarta.persistence.EntityManager;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class PostgresGraphExecutor {

    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;

    public PostgresGraphExecutor(
            final EntityManager entityManager,
            final PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
    }

    public DbGraphResult execute(final DbGraphContext context, final List<DbContractInvocation<?>> chain) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        transactionTemplate.setPropagationBehavior(this.resolvePropagation().value());

        return Objects.requireNonNull(transactionTemplate.execute(status -> this.executeGraph(context, chain)));
    }

    private DbGraphResult executeGraph(final DbGraphContext context, final List<DbContractInvocation<?>> chain) {
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

    private Propagation resolvePropagation() {
        final Class<?> testClass = TestRollbackContextHolder.getCurrentTestClass();
        if (testClass == null) {
            return Propagation.REQUIRED;
        }

        final Rollback rollback = AnnotatedElementUtils.findMergedAnnotation(testClass, Rollback.class);
        final boolean rollbackEnabled = rollback == null || rollback.value();

        if (rollbackEnabled) {
            return Propagation.MANDATORY;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return Propagation.MANDATORY;
        }

        return Propagation.REQUIRED;
    }
}
