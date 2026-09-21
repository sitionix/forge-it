package com.sitionix.forgeit.ros.internal.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sitionix.forgeit.ros.api.RosQos;
import com.sitionix.forgeit.ros.internal.transport.RosSubscription;
import com.sitionix.forgeit.ros.internal.transport.RosTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class RosLifecycleTest {
    @Test void shutdownFailureEscapesSpringAfterClassAndEvictsContext() {
        var manager = new TestContextManager(FailingTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        assertThatThrownBy(manager::afterTestClass).isSameAs(transport.failure);
        assertThat(context.isActive()).isFalse();
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
    }

    @Test void shutdownFailureDoesNotReplaceAnEarlierAfterClassFailure() {
        var manager = new TestContextManager(AlreadyFailingTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        assertThatThrownBy(manager::afterTestClass).isSameAs(PrimaryFailure.failure)
                .satisfies(error -> assertThat(error.getSuppressed()).contains(transport.failure));
        assertThat(context.isActive()).isFalse();
    }

    @Test void successfulShutdownAlsoEvictsContext() throws Exception {
        var manager = new TestContextManager(SuccessfulTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        manager.afterTestClass();
        assertThat(transport.closed).isTrue();
        assertThat(context.isActive()).isFalse();
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
    }

    @Test void doesNotCreateAnUnusedContext() throws Exception {
        var manager = new TestContextManager(FailingTest.class);
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
        manager.afterTestClass();
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
    }

    @Test void leavesNonRosContextCached() throws Exception {
        var manager = new TestContextManager(NonRosTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        try {
            manager.afterTestClass();
            assertThat(context.isActive()).isTrue();
            assertThat(manager.getTestContext().getApplicationContext()).isSameAs(context);
        } finally {
            manager.getTestContext().markApplicationContextDirty(DirtiesContext.HierarchyMode.CURRENT_LEVEL);
        }
    }

    @ParameterizedTest
    @MethodSource("methodEvictions")
    void shutdownFailureEscapesMethodEviction(Class<?> testClass, String methodName, boolean before) throws Exception {
        var manager = new TestContextManager(testClass);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        var instance = testClass.getDeclaredConstructor().newInstance();
        var method = testClass.getDeclaredMethod(methodName);
        assertThatThrownBy(() -> {
            if (before) manager.beforeTestMethod(instance, method);
            else manager.afterTestMethod(instance, method, null);
        }).isSameAs(transport.failure);
        assertThat(manager.getTestContext().getAttribute(
                DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(context.isActive()).isFalse();
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
    }

    static Stream<Arguments> methodEvictions() {
        return Stream.of(
                Arguments.of(MethodDirtyTest.class, "beforeDirty", true),
                Arguments.of(MethodDirtyTest.class, "afterDirty", false),
                Arguments.of(BeforeEachDirtyTest.class, "ordinary", true),
                Arguments.of(AfterEachDirtyTest.class, "ordinary", false));
    }

    @Test void shutdownFailureBeforeClassEvictionEscapes() {
        var manager = new TestContextManager(BeforeClassDirtyTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        assertThatThrownBy(manager::beforeTestClass).isSameAs(transport.failure);
        assertThat(context.isActive()).isFalse();
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
    }

    @Test void ordinaryMethodsKeepTheirTransportUntilAfterClass() throws Exception {
        var manager = new TestContextManager(SuccessfulTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        try {
            var instance = new SuccessfulTest();
            var method = SuccessfulTest.class.getDeclaredMethod("ordinary");
            manager.beforeTestMethod(instance, method);
            manager.afterTestMethod(instance, method, null);
            assertThat(transport.closed).isFalse();
            assertThat(context.isActive()).isTrue();
            assertThat(manager.getTestContext().getApplicationContext()).isSameAs(context);
        } finally {
            manager.afterTestClass();
        }
    }

    @Test void methodEvictionKeepsEarlierListenerFailureAsPrimary() throws Exception {
        var manager = new TestContextManager(AlreadyFailingTest.class);
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        var transport = context.getBean(FakeTransport.class);
        var instance = new AlreadyFailingTest();
        var method = AlreadyFailingTest.class.getDeclaredMethod("afterDirty");
        assertThatThrownBy(() -> manager.afterTestMethod(instance, method, new AssertionError("test body failed")))
                .isSameAs(PrimaryFailure.methodFailure)
                .satisfies(error -> assertThat(error.getSuppressed()).contains(transport.failure));
        assertThat(context.isActive()).isFalse();
    }

    @ContextConfiguration(classes = FailingConfiguration.class)
    static class MethodDirtyTest {
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD) void beforeDirty() { }
        @DirtiesContext void afterDirty() { }
    }

    @ContextConfiguration(classes = FailingConfiguration.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
    static class BeforeEachDirtyTest { void ordinary() { } }

    @ContextConfiguration(classes = FailingConfiguration.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    static class AfterEachDirtyTest { void ordinary() { } }

    @ContextConfiguration(classes = FailingConfiguration.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    static class BeforeClassDirtyTest { }

    @ContextConfiguration(classes = FailingConfiguration.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    static class FailingTest { }

    @ContextConfiguration(classes = FailingConfiguration.class)
    @TestExecutionListeners(listeners = PrimaryFailure.class,
            mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
    static class AlreadyFailingTest { @DirtiesContext void afterDirty() { } }

    @ContextConfiguration(classes = SuccessfulConfiguration.class)
    static class SuccessfulTest { void ordinary() { } }

    @ContextConfiguration(classes = EmptyConfiguration.class)
    static class NonRosTest { }

    @Configuration(proxyBeanMethods = false)
    static class FailingConfiguration {
        @Bean FakeTransport transport() { return new FakeTransport(true); }
    }

    @Configuration(proxyBeanMethods = false)
    static class SuccessfulConfiguration {
        @Bean FakeTransport transport() { return new FakeTransport(false); }
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration { }

    public static class PrimaryFailure extends AbstractTestExecutionListener {
        static final IllegalStateException failure = new IllegalStateException("original after-class failure");
        static final IllegalStateException methodFailure = new IllegalStateException("original after-method failure");
        @Override public int getOrder() { return 4001; }
        @Override public void afterTestClass(TestContext context) { throw failure; }
        @Override public void afterTestMethod(TestContext context) { throw methodFailure; }
    }

    static class FakeTransport implements RosTransport {
        final IllegalStateException failure = new IllegalStateException("ROS shutdown failed");
        final boolean fail;
        boolean closed;
        FakeTransport(boolean fail) { this.fail = fail; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (fail) throw failure;
        }
        @Override public RosSubscription subscribe(String topic, String type, RosQos qos, Duration timeout) {
            throw new UnsupportedOperationException();
        }
        @Override public void publish(String topic, String type, RosQos qos, JsonNode message, Duration timeout) {
            throw new UnsupportedOperationException();
        }
    }
}
