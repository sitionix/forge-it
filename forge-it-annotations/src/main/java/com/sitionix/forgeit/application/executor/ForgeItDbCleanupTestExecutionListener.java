package com.sitionix.forgeit.application.executor;

import com.sitionix.forgeit.core.contract.DbCleanup;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsRegistry;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.List;
import java.util.Map;

@Slf4j
public final class ForgeItDbCleanupTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 3000;
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        final CleanupPhase phase = this.resolveClassPhase(testContext);
        if (phase == CleanupPhase.BEFORE_ALL) {
            this.performCleanup(testContext);
        }
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        final CleanupPhase phase = this.resolveEffectivePhase(testContext);
        if (phase == CleanupPhase.BEFORE_EACH) {
            this.performCleanup(testContext);
        }
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        final CleanupPhase phase = this.resolveEffectivePhase(testContext);
        if (phase == CleanupPhase.AFTER_EACH) {
            this.performCleanupSafely(testContext);
        }
    }

    @Override
    public void afterTestClass(final TestContext testContext) {
        final CleanupPhase phase = this.resolveClassPhase(testContext);
        if (phase == CleanupPhase.AFTER_ALL) {
            this.performCleanup(testContext);
        }
    }

    private void performCleanup(final TestContext testContext) {
        final Map<String, DbCleaner> cleaners =
                testContext.getApplicationContext().getBeansOfType(DbCleaner.class);
        if (cleaners.isEmpty()) {
            log.debug("Skipping DB cleanup because no DbCleaner beans are registered.");
            return;
        }

        final Map<String, DbContractsRegistry> registries =
                testContext.getApplicationContext().getBeansOfType(DbContractsRegistry.class);
        final List<DbContract<?>> contracts = registries.values()
                .stream()
                .findFirst()
                .map(DbContractsRegistry::allContracts)
                .orElse(List.of());
        cleaners.values().forEach(cleaner -> cleaner.clearTables(contracts));
    }

    private void performCleanupSafely(final TestContext testContext) {
        final Throwable testException = testContext.getTestException();
        try {
            this.performCleanup(testContext);
        } catch (RuntimeException ex) {
            if (testException != null) {
                log.error("DB cleanup after test {} failed; original test exception preserved.",
                        testContext.getTestMethod(), ex);
                return;
            }
            throw ex;
        }
    }

    private CleanupPhase resolveClassPhase(final TestContext testContext) {
        final Class<?> testClass = testContext.getTestClass();
        final DbCleanup annotation =
                AnnotatedElementUtils.findMergedAnnotation(testClass, DbCleanup.class);
        if (annotation == null) {
            return CleanupPhase.NONE;
        }
        return annotation.phase();
    }

    private CleanupPhase resolveEffectivePhase(final TestContext testContext) {
        final CleanupPhase methodPhase = this.resolveMethodPhase(testContext);
        if (methodPhase != null && methodPhase != CleanupPhase.NONE) {
            return methodPhase;
        }
        return this.resolveClassPhase(testContext);
    }

    @Nullable
    private CleanupPhase resolveMethodPhase(final TestContext testContext) {
        testContext.getTestMethod();
        final DbCleanup annotation = AnnotatedElementUtils.findMergedAnnotation(
                testContext.getTestMethod(),
                DbCleanup.class
        );
        if (annotation == null) {
            return null;
        }
        return annotation.phase();
    }
}
