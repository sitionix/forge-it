package com.sitionix.forgeit.ros.internal.test;

import com.sitionix.forgeit.ros.internal.transport.RosTransport;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractDirtiesContextTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/** Makes ROS cleanup failures visible to JUnit instead of Spring's bean-destruction logger. */
public final class RosTestExecutionListener extends AbstractDirtiesContextTestExecutionListener {
    @Override public int getOrder() {
        // After callbacks run in reverse order: close before @DirtiesContext destroys beans.
        return 3001;
    }

    @Override public void afterTestMethod(TestContext testContext) throws Exception {
        beforeOrAfterTestMethod(testContext, DirtiesContext.MethodMode.AFTER_METHOD,
                DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD);
    }

    @Override public void afterTestClass(TestContext testContext) {
        closeAndEvict(testContext, DirtiesContext.HierarchyMode.CURRENT_LEVEL);
    }

    @Override protected void dirtyContext(TestContext testContext, DirtiesContext.HierarchyMode hierarchyMode) {
        closeAndEvict(testContext, hierarchyMode);
    }

    private static void closeAndEvict(TestContext testContext, DirtiesContext.HierarchyMode hierarchyMode) {
        if (!testContext.hasApplicationContext()) return;
        var context = testContext.getApplicationContext();
        String[] names = context.getBeanNamesForType(RosTransport.class, false, false);
        if (names.length == 0) return;

        Throwable failure = null;
        for (String name : names) {
            try {
                context.getBean(name, RosTransport.class).close();
            } catch (Throwable error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        try {
            // ROS customizers make this context class-owned. Never cache a closed transport,
            // including when close failed; its idempotent bean-destruction callback is safe.
            testContext.markApplicationContextDirty(hierarchyMode);
        } catch (Throwable error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        // Before callbacks stop on failure, so Spring's standard dirties listener may not run.
        // Keep dependency reinjection enabled for the next method even in that case.
        testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
                Boolean.TRUE);
        // TestContextManager preserves earlier lifecycle failures and suppresses this one.
        if (failure != null) ReflectionUtils.rethrowRuntimeException(failure);
    }

    /** Before callbacks need a separate order, preceding Spring's before-dirties listener (1500). */
    public static final class BeforeDirtiesContext extends AbstractDirtiesContextTestExecutionListener {
        @Override public int getOrder() { return 1499; }

        @Override public void beforeTestClass(TestContext testContext) throws Exception {
            beforeOrAfterTestClass(testContext, DirtiesContext.ClassMode.BEFORE_CLASS);
        }

        @Override public void beforeTestMethod(TestContext testContext) throws Exception {
            beforeOrAfterTestMethod(testContext, DirtiesContext.MethodMode.BEFORE_METHOD,
                    DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD);
        }

        @Override protected void dirtyContext(TestContext testContext, DirtiesContext.HierarchyMode hierarchyMode) {
            closeAndEvict(testContext, hierarchyMode);
        }
    }
}
