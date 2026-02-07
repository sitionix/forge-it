package com.sitionix.forgeit.wiremock.internal.executor;

import com.sitionix.forgeit.annotation.ForgeDataPreparation;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.domain.preparation.DataPreparation;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class WireMockResetTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(WireMockResetTestExecutionListener.class);
    private static final String FORGE_IT_FQN = "com.sitionix.forgeit.core.api.ForgeIT";

    @Override
    public int getOrder() {
        return 3100;
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        this.reset(testContext);
        this.runPreparations(testContext);
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        this.resetSafely(testContext);
    }

    private void resetSafely(final TestContext testContext) {
        final Throwable testException = testContext.getTestException();
        try {
            this.reset(testContext);
        } catch (RuntimeException ex) {
            if (testException != null) {
                log.error("WireMock reset after test {} failed; original test exception preserved.",
                        testContext.getTestMethod(), ex);
                return;
            }
            throw ex;
        }
    }

    private void reset(final TestContext testContext) {
        if (testContext.getApplicationContext().getBeansOfType(WireMockJournal.class).isEmpty()) {
            log.debug("Skipping WireMock reset because WireMock support is not registered.");
            return;
        }
        testContext.getApplicationContext().getBean(WireMockJournal.class).reset();
    }

    private void runPreparations(final TestContext testContext) {
        final Class<?> testClass = testContext.getTestClass();
        final Set<Class<? extends DataPreparation<?>>> preparations = new LinkedHashSet<>();
        final IntegrationTest integrationTest =
                AnnotatedElementUtils.findMergedAnnotation(testClass, IntegrationTest.class);
        if (integrationTest != null) {
            Collections.addAll(preparations, integrationTest.preparations());
        }
        final ForgeDataPreparation forgeDataPreparation =
                AnnotatedElementUtils.findMergedAnnotation(testClass, ForgeDataPreparation.class);
        if (forgeDataPreparation != null) {
            Collections.addAll(preparations, forgeDataPreparation.value());
        }
        if (preparations.isEmpty()) {
            return;
        }

        final Object forgeIt = this.resolveForgeIt(testContext);
        for (final Class<? extends DataPreparation<?>> preparationClass : preparations) {
            final DataPreparation<?> preparation = this.instantiatePreparation(preparationClass);
            this.invokePreparation(preparation, forgeIt);
        }
    }

    private DataPreparation<?> instantiatePreparation(final Class<? extends DataPreparation<?>> preparationClass) {
        try {
            return preparationClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to instantiate DataPreparation " + preparationClass.getName(), ex);
        }
    }

    private void invokePreparation(final DataPreparation<?> preparation, final Object forgeIt) {
        try {
            @SuppressWarnings("unchecked")
            final DataPreparation<Object> typed = (DataPreparation<Object>) preparation;
            typed.prepare(forgeIt);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to run DataPreparation " + preparation.getClass().getName(), ex);
        }
    }

    private Object resolveForgeIt(final TestContext testContext) {
        final Class<?> testClass = testContext.getTestClass();
        final Class<?> forgeItType = this.resolveForgeItType();
        final Set<Class<?>> candidates = new LinkedHashSet<>();
        ReflectionUtils.doWithFields(testClass, field -> {
            if (!Modifier.isStatic(field.getModifiers()) && forgeItType.isAssignableFrom(field.getType())) {
                candidates.add(field.getType());
            }
        });
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No ForgeIT contract found on test class " + testClass.getName());
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Multiple ForgeIT contracts detected on test class " + testClass.getName());
        }
        final Class<?> contractType = candidates.iterator().next();
        return testContext.getApplicationContext().getBean(contractType);
    }

    private Class<?> resolveForgeItType() {
        try {
            return ClassUtils.forName(FORGE_IT_FQN, ClassUtils.getDefaultClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("ForgeIT type not found on classpath", ex);
        }
    }
}
