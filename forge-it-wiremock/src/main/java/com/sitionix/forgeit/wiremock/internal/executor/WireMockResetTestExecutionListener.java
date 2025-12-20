package com.sitionix.forgeit.wiremock.internal.executor;

import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public final class WireMockResetTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(WireMockResetTestExecutionListener.class);

    @Override
    public int getOrder() {
        return 3100;
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
}
