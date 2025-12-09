package com.sitionix.forgeit.application.executor;

import com.sitionix.forgeit.core.contract.DbCleanup;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsRegistry;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.List;

public final class ForgeItDbCleanupTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 3000;
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        this.validateConfiguration(testContext.getTestClass());
        final CleanupPhase phase = this.resolveClassPhase(testContext);
        if (phase == CleanupPhase.BEFORE_ALL) {
            this.performCleanup(testContext);
        }
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        this.validateConfiguration(testContext.getTestClass());
        final CleanupPhase phase = this.resolveEffectivePhase(testContext);
        if (phase == CleanupPhase.BEFORE_EACH) {
            this.performCleanup(testContext);
        }
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        this.validateConfiguration(testContext.getTestClass());
        final CleanupPhase phase = this.resolveEffectivePhase(testContext);
        if (phase == CleanupPhase.AFTER_EACH) {
            this.performCleanup(testContext);
        }
    }

    @Override
    public void afterTestClass(final TestContext testContext) {
        this.validateConfiguration(testContext.getTestClass());
        final CleanupPhase phase = this.resolveClassPhase(testContext);
        if (phase == CleanupPhase.AFTER_ALL) {
            this.performCleanup(testContext);
        }
    }

    private void performCleanup(final TestContext testContext) {
        final DbContractsRegistry registry = testContext.getApplicationContext()
                .getBean(DbContractsRegistry.class);

        final DbCleaner cleaner = testContext.getApplicationContext()
                .getBean(DbCleaner.class);

        final List<DbContract<?>> contracts = registry.allContracts();
        cleaner.clearTables(contracts);
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

    private void validateConfiguration(final Class<?> testClass) {
        final DbCleanup classCleanup = AnnotatedElementUtils.findMergedAnnotation(testClass, DbCleanup.class);
        final CleanupPhase phase = classCleanup != null ? classCleanup.phase() : CleanupPhase.NONE;

        final Rollback rollback = AnnotatedElementUtils.findMergedAnnotation(testClass, Rollback.class);
        final boolean rollbackEnabled = rollback == null || rollback.value();

        if (rollbackEnabled && phase != CleanupPhase.NONE) {
            throw new IllegalStateException("""
                    ForgeIT configuration error for test class %s: rollback=true and DbCleanup.phase=%s.%n
                    This combination leads to flaky tests.%n
                    Please choose ONE of the following:%n
                      - rollback=true  and DbCleanup.phase=NONE  (Spring-style transactional tests)%n
                      - rollback=false and DbCleanup.phase!=NONE (ForgeIT DB cleanup mode)
                    """.formatted(testClass.getName(), phase));

        }
    }
}