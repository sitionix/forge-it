package com.sitionix.forgeit.application.executor;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures transactional tests are detected even when {@link Transactional} is declared via composed annotations.
 */
public class ForgeItTransactionalTestExecutionListener extends TransactionalTestExecutionListener {

    @Override
    protected boolean isTestMethodTransactional(final TestContext testContext) {
        return super.isTestMethodTransactional(testContext)
                || AnnotatedElementUtils.hasAnnotation(testContext.getTestMethod(), Transactional.class);
    }

    @Override
    protected boolean isClassTransactional(final TestContext testContext) {
        return super.isClassTransactional(testContext)
                || AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), Transactional.class);
    }
}
