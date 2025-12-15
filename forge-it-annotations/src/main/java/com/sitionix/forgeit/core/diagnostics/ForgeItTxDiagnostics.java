package com.sitionix.forgeit.core.diagnostics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ForgeItTxDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(ForgeItTxDiagnostics.class);

    private ForgeItTxDiagnostics() {
    }

    public static void logTestPhaseSnapshot(final String phaseLabel, final TestContext testContext) {
        final ApplicationContext context = testContext.getApplicationContext();
        final String diagnostics = buildDiagnostics(phaseLabel, context, testContext, null);
        log.info("{}\n{}", phaseLabel, diagnostics);
    }

    public static EntityManager requireTransactionalEntityManager(
            final ApplicationContext context,
            final EntityManagerFactory executorEmf,
            final String phaseLabel
    ) {
        final boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        final boolean resourceBound = TransactionSynchronizationManager.hasResource(executorEmf);
        final EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(executorEmf);

        final Optional<JpaTransactionManager> jpaTm = resolveJpaTransactionManager(context);
        final EntityManagerFactory tmEmf = jpaTm.map(JpaTransactionManager::getEntityManagerFactory).orElse(null);

        final boolean emfMismatch = tmEmf != null && tmEmf != executorEmf;

        if (!txActive || !resourceBound || em == null || emfMismatch) {
            final StringBuilder message = new StringBuilder("ForgeIT transactional prerequisites are not satisfied: \n")
                    .append(" - Spring tx active: ").append(txActive).append('\n')
                    .append(" - EMF resource bound: ").append(resourceBound).append('\n')
                    .append(" - Transactional EntityManager present: ").append(em != null).append('\n');

            if (jpaTm.isPresent()) {
                message.append(" - JpaTransactionManager bean: ")
                        .append(jpaTm.get().getClass().getName())
                        .append(" using EMF @")
                        .append(identity(tmEmf))
                        .append('\n');
            } else {
                message.append(" - No JpaTransactionManager found among beans\n");
            }

            if (emfMismatch) {
                message.append(" - TM uses EMF @").append(identity(tmEmf))
                        .append(" but executor uses EMF @")
                        .append(identity(executorEmf)).append('\n');
            }

            message.append("\nDiagnostics:\n")
                    .append(buildDiagnostics(phaseLabel, context, null, executorEmf));

            throw new IllegalStateException(message.toString());
        }

        log.info("{}\n{}", phaseLabel, buildDiagnostics(phaseLabel, context, null, executorEmf));
        return em;
    }

    private static String buildDiagnostics(
            final String phaseLabel,
            final ApplicationContext context,
            @Nullable final TestContext testContext,
            @Nullable final EntityManagerFactory executorEmf
    ) {
        final List<String> tmBeans = context.getBeansOfType(PlatformTransactionManager.class)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> " - %s -> %s".formatted(entry.getKey(), describe(entry.getValue())))
                .toList();

        final List<String> emfBeans = context.getBeansOfType(EntityManagerFactory.class)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> " - %s -> %s".formatted(entry.getKey(), describe(entry.getValue())))
                .toList();

        final Map<Object, Object> resourceMap = TransactionSynchronizationManager.getResourceMap();
        final List<String> resources = resourceMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> describe(entry.getKey())))
                .map(entry -> " - %s => %s".formatted(describe(entry.getKey()), describe(entry.getValue())))
                .toList();

        final String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        final Integer isolation = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        final boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        final boolean actualActive = TransactionSynchronizationManager.isActualTransactionActive();
        final boolean syncActive = TransactionSynchronizationManager.isSynchronizationActive();

        final List<String> mergedTxAnnotations = resolveTransactionalAnnotations(testContext);
        final List<String> listeners = resolveListeners(testContext);

        final StringBuilder builder = new StringBuilder();
        builder.append("[ForgeIT Tx Diagnostics] ").append(phaseLabel).append('\n');
        builder.append("Transaction status:\n")
                .append(" - actualActive=").append(actualActive)
                .append(", synchronizationActive=").append(syncActive)
                .append(", name=").append(txName)
                .append(", isolation=").append(isolation)
                .append(", readOnly=").append(readOnly)
                .append('\n');

        builder.append("PlatformTransactionManager beans:\n");
        tmBeans.forEach(line -> builder.append(line).append('\n'));

        builder.append("EntityManagerFactory beans:\n");
        emfBeans.forEach(line -> builder.append(line).append('\n'));

        if (executorEmf != null) {
            builder.append("Executor EMF identity: ").append(describe(executorEmf)).append('\n');
        }

        builder.append("TransactionSynchronizationManager resourceMap:\n");
        resources.forEach(line -> builder.append(line).append('\n'));

        if (!mergedTxAnnotations.isEmpty()) {
            builder.append("@Transactional merged on test class/method:\n");
            mergedTxAnnotations.forEach(line -> builder.append(" - ").append(line).append('\n'));
        }

        if (!listeners.isEmpty()) {
            builder.append("Active TestExecutionListeners (order as registered):\n");
            listeners.forEach(line -> builder.append(" - ").append(line).append('\n'));
        }

        return builder.toString();
    }

    private static List<String> resolveTransactionalAnnotations(@Nullable final TestContext testContext) {
        if (testContext == null) {
            return List.of();
        }

        final List<String> result = new ArrayList<>();
        final Class<?> testClass = testContext.getTestClass();
        final var classAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                testClass,
                org.springframework.transaction.annotation.Transactional.class
        );
        if (classAnnotation != null) {
            result.add("class %s -> manager=%s, propagation=%s, readOnly=%s"
                    .formatted(
                            testClass.getName(),
                            classAnnotation.transactionManager(),
                            classAnnotation.propagation(),
                            classAnnotation.readOnly()
                    ));
        }

        if (testContext.getTestMethod() != null) {
            final var methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                    testContext.getTestMethod(),
                    org.springframework.transaction.annotation.Transactional.class
            );
            if (methodAnnotation != null) {
                result.add("method %s -> manager=%s, propagation=%s, readOnly=%s"
                        .formatted(
                                testContext.getTestMethod().getName(),
                                methodAnnotation.transactionManager(),
                                methodAnnotation.propagation(),
                                methodAnnotation.readOnly()
                        ));
            }
        }

        return result;
    }

    private static List<String> resolveListeners(@Nullable final TestContext testContext) {
        if (testContext == null) {
            return List.of();
        }

        try {
            final Object manager = ReflectionTestUtils.getField(testContext, "testContextManager");
            if (manager == null) {
                return List.of("unavailable: testContextManager not exposed on TestContext");
            }

            final Object listeners = ReflectionTestUtils.invokeMethod(manager, "getTestExecutionListeners");
            if (!(listeners instanceof TestExecutionListener[] typed)) {
                return List.of("unavailable: unexpected listener container type %s".formatted(
                        listeners == null ? "null" : listeners.getClass().getName()
                ));
            }

            return Arrays.stream(typed)
                    .filter(Objects::nonNull)
                    .map(listener -> listener.getClass().getName())
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException ex) {
            return List.of("unavailable: " + ex.getMessage());
        }
    }

    private static Optional<JpaTransactionManager> resolveJpaTransactionManager(final ApplicationContext context) {
        final Map<String, JpaTransactionManager> jpaTms = context.getBeansOfType(JpaTransactionManager.class);
        if (jpaTms.isEmpty()) {
            return Optional.empty();
        }
        if (jpaTms.size() == 1) {
            return Optional.of(jpaTms.values().iterator().next());
        }
        if (jpaTms.containsKey("transactionManager")) {
            return Optional.of(jpaTms.get("transactionManager"));
        }
        return Optional.of(jpaTms.values().iterator().next());
    }

    private static String describe(final Object bean) {
        if (bean == null) {
            return "null";
        }
        return bean.getClass().getName() + "@" + identity(bean);
    }

    private static String identity(final Object bean) {
        if (bean == null) {
            return "null";
        }
        return Integer.toHexString(System.identityHashCode(bean));
    }
}
