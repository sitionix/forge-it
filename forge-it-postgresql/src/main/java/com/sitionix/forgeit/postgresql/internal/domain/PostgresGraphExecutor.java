package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.graph.DbGraphContext;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostgresGraphExecutor {

    private final EntityManagerFactory emf;

    public PostgresGraphExecutor(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public DbGraphResult execute(DbGraphContext context, List<DbContractInvocation<?>> chain) {
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
        if (em == null) {
            throw new IllegalStateException("No transactional EntityManager bound to current thread");
        }

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

