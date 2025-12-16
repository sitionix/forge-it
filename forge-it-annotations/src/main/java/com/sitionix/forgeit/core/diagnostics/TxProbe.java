package com.sitionix.forgeit.core.diagnostics;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TxProbe {

    private TxProbe() {
    }

    public static String describe(final Object bean) {
        if (bean == null) {
            return "null";
        }
        return bean.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(bean));
    }

    public static String snapshot(final String where) {
        return String.format(
                "[TX-PROBE] %s thread=%s actual=%s sync=%s name=%s resources=%s",
                where,
                Thread.currentThread().getName(),
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.isSynchronizationActive(),
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.getResourceMap().keySet()
        );
    }

    public static List<String> describeTransactionManagers(final ApplicationContext context) {
        return context.getBeansOfType(PlatformTransactionManager.class)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> describeTransactionManager(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public static List<String> describeEntityManagerFactories(final ApplicationContext context) {
        return context.getBeansOfType(EntityManagerFactory.class)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "%s=%s".formatted(entry.getKey(), describe(entry.getValue())))
                .collect(Collectors.toList());
    }

    private static String describeTransactionManager(final String beanName, final PlatformTransactionManager tm) {
        final StringBuilder builder = new StringBuilder(beanName).append("=").append(describe(tm));
        if (tm instanceof final JpaTransactionManager jpaTransactionManager) {
            builder.append(" emf=").append(describe(jpaTransactionManager.getEntityManagerFactory()));
        }
        return builder.toString();
    }
}
