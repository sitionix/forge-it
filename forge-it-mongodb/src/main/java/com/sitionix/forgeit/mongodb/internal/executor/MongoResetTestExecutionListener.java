package com.sitionix.forgeit.mongodb.internal.executor;

import com.sitionix.forgeit.mongodb.internal.cleaner.MongoCollectionCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public final class MongoResetTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(MongoResetTestExecutionListener.class);

    @Override
    public int getOrder() {
        return 3050;
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        this.resetSafely(testContext, "after");
    }

    private void resetSafely(final TestContext testContext, final String phase) {
        final Throwable testException = testContext.getTestException();
        try {
            this.reset(testContext);
        } catch (final RuntimeException ex) {
            if (testException != null) {
                log.error("Mongo reset {} test {} failed; original test exception preserved.",
                        phase, testContext.getTestMethod(), ex);
                return;
            }
            throw ex;
        }
    }

    private void reset(final TestContext testContext) {
        if (testContext.getApplicationContext().getBeansOfType(MongoCollectionCleaner.class).isEmpty()) {
            log.debug("Skipping Mongo reset because Mongo support is not registered.");
            return;
        }
        testContext.getApplicationContext().getBean(MongoCollectionCleaner.class).reset();
    }
}
