package com.sitionix.forgeit.postgresql.internal.domain;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TxAroundProbeAspect {

    private static final Logger log = LoggerFactory.getLogger(TxAroundProbeAspect.class);

    private final PlatformTransactionManager transactionManager;

    public TxAroundProbeAspect(final PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Around("execution(* com.sitionix.forgeit.postgresql.internal.domain.PostgresGraphExecutor.execute(..))")
    public Object around(final ProceedingJoinPoint pjp) throws Throwable {
        Object emfKey = null;
        boolean hasEmfResource = false;

        if (this.transactionManager instanceof final JpaTransactionManager jpaTm) {
            emfKey = jpaTm.getEntityManagerFactory();
            hasEmfResource = TransactionSynchronizationManager.hasResource(emfKey);
        }

        log.info("[TX-AROUND] before TM={} actual={} sync={} name={} hasEmfResource={} emfKey={} resourceKeys={}",
                this.id(this.transactionManager),
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.isSynchronizationActive(),
                TransactionSynchronizationManager.getCurrentTransactionName(),
                hasEmfResource,
                this.id(emfKey),
                TransactionSynchronizationManager.getResourceMap().keySet().stream().map(this::id).toList()
        );

        return pjp.proceed();
    }

    private String id(final Object target) {
        if (target == null) {
            return "null";
        }
        return target.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(target));
    }
}
