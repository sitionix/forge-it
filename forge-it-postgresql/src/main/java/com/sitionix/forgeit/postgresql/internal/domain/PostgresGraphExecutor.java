package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.core.diagnostics.TxProbe;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostgresGraphExecutor {

    private static final Logger log = LoggerFactory.getLogger(PostgresGraphExecutor.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ApplicationContext context;

    @Transactional(propagation = Propagation.MANDATORY)
    public DbGraphResult execute(DbGraphContext context, List<DbContractInvocation<?>> chain) {
        log.info(TxProbe.snapshot("executor-execute-entry"));
        log.info("[EXECUTOR] aopProxy={} cglibProxy={} class={} id={} bfId={} tmId={} emfId={} resources={}",
                AopUtils.isAopProxy(this),
                AopUtils.isCglibProxy(this),
                this.getClass().getName(),
                System.identityHashCode(this),
                System.identityHashCode(this.context.getAutowireCapableBeanFactory()),
                System.identityHashCode(this.context.getBean("transactionManager")),
                System.identityHashCode(this.context.getBean("entityManagerFactory")),
                TransactionSynchronizationManager.getResourceMap().keySet().stream()
                        .map(key -> key.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(key)))
                        .toList());

        for (DbContractInvocation<?> invocation : chain) {
            context.getOrCreate(invocation);
        }

        Map<DbContract<?>, Object> original = context.snapshot();
        Map<DbContract<?>, Object> managedMap = new LinkedHashMap<>();

        for (var entry : original.entrySet()) {
            Object entity = entry.getValue();
            if (entity == null) {
                managedMap.put(entry.getKey(), null);
                continue;
            }
            Object managed = em.contains(entity) ? entity : em.merge(entity);
            managedMap.put(entry.getKey(), managed);
        }

        em.flush();
        return new DefaultDbGraphResult(Map.copyOf(managedMap));
    }
}

