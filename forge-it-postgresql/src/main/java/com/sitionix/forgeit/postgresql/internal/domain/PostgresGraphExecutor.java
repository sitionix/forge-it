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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PostgresGraphExecutor {

    private static final Logger log = LoggerFactory.getLogger(PostgresGraphExecutor.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ApplicationContext context;

    @Transactional(propagation = Propagation.MANDATORY)
    public DbGraphResult execute(DbGraphContext context, List<DbContractInvocation<?>> chain) {
        log.info("[TX-PROBE] executor-entry thread={} actual={} name={} resources={}",
                Thread.currentThread().getName(),
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.getResourceMap().keySet());
        log.info(TxProbe.snapshot("executor-execute-entry"));
        log.info("[TX-PROBE] resourceKeys={}",
                TransactionSynchronizationManager.getResourceMap().keySet().stream().map(PostgresGraphExecutor::resKey).toList());
        log.info("[TX-PROBE] executor-beans tmByName={} tmPrimaryCandidates={} emfBeanNames={}",
                TxProbe.describe(this.context.getBean("transactionManager")),
                this.context.getBeansOfType(org.springframework.transaction.PlatformTransactionManager.class)
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + TxProbe.describe(entry.getValue()))
                        .collect(Collectors.toList()),
                Arrays.toString(this.context.getBeanNamesForType(jakarta.persistence.EntityManagerFactory.class)));
        log.info("[CTX] bfId={}", System.identityHashCode(((ConfigurableApplicationContext) this.context).getBeanFactory()));
        log.info("[EXECUTOR] aopProxy={} cglibProxy={} class={} id={}",
                AopUtils.isAopProxy(this),
                AopUtils.isCglibProxy(this),
                this.getClass().getName(),
                System.identityHashCode(this));

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

    private static String resKey(Object key) {
        return key.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(key));
    }
}

