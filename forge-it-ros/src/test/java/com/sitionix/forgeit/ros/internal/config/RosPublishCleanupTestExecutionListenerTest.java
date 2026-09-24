package com.sitionix.forgeit.ros.internal.config;

import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RosPublishCleanupTestExecutionListenerTest {

    @E2E
    static class Scenario { }

    @Test
    void listenerIsRegisteredForE2eMethods() {
        assertTrue(new TestContextManager(Scenario.class).getTestExecutionListeners().stream()
                .anyMatch(RosPublishCleanupTestExecutionListener.class::isInstance));
    }

    @Test
    void methodCompletionStopsPeriodicPublishers() {
        final RosPublisherPort publisher = mock(RosPublisherPort.class);
        final TestContext testContext = mock(TestContext.class);
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(RosPublisherPort.class, () -> publisher);
            context.refresh();
            when(testContext.getApplicationContext()).thenReturn(context);
            new RosPublishCleanupTestExecutionListener().afterTestExecution(testContext);
            verify(publisher).stopPeriodic();
        }
    }
}
