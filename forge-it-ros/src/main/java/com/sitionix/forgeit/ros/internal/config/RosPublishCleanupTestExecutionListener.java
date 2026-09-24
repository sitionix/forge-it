package com.sitionix.forgeit.ros.internal.config;

import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/** Stops method-scoped periodic publications before JUnit afterEach lifecycle cleanup. */
public final class RosPublishCleanupTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestExecution(final TestContext testContext) {
        testContext.getApplicationContext().getBeansOfType(RosPublisherPort.class)
                .values().forEach(RosPublisherPort::stopPeriodic);
    }
}
