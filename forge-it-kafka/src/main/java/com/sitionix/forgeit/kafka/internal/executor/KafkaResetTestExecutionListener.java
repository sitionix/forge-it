package com.sitionix.forgeit.kafka.internal.executor;

import com.sitionix.forgeit.kafka.internal.cleaner.KafkaTopicCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public final class KafkaResetTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaResetTestExecutionListener.class);

    @Override
    public int getOrder() {
        return 3050;
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        this.resetSafely(testContext);
    }

    private void resetSafely(final TestContext testContext) {
        final Throwable testException = testContext.getTestException();
        try {
            this.reset(testContext);
        } catch (final RuntimeException ex) {
            if (testException != null) {
                log.error("Kafka reset before test {} failed; original test exception preserved.",
                        testContext.getTestMethod(), ex);
                return;
            }
            throw ex;
        }
    }

    private void reset(final TestContext testContext) {
        if (testContext.getApplicationContext().getBeansOfType(KafkaTopicCleaner.class).isEmpty()) {
            log.debug("Skipping Kafka reset because Kafka support is not registered.");
            return;
        }
        testContext.getApplicationContext().getBean(KafkaTopicCleaner.class).reset();
    }
}
