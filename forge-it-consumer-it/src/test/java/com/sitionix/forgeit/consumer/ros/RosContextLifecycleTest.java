package com.sitionix.forgeit.consumer.ros;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.ros.api.RosQos;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RosContextLifecycleTest {

    @Test
    void adapterIsReusedAcrossClassesAndClosesWithItsContext() throws Exception {
        ProtocolFixture.python = Files.createTempFile("forge-ros-context-", ".py");
        final TestContextManager first = new TestContextManager(FirstRos.class);
        final TestContextManager second = new TestContextManager(SecondRos.class);
        try {
            try (final var resource = this.getClass().getResourceAsStream("/ros-context-adapter.py")) {
                Files.copy(resource, ProtocolFixture.python, StandardCopyOption.REPLACE_EXISTING);
            }
            assertThat(ProtocolFixture.python.toFile().setExecutable(true, true)).isTrue();
            final var context = (ConfigurableApplicationContext) first.getTestContext().getApplicationContext();
            assertThat(second.getTestContext().getApplicationContext()).isSameAs(context);
            final RosPublisherPort publisher = context.getBean(RosPublisherPort.class);
            assertThat(context.getBean(RosConsumerPort.class)).isSameAs(publisher);

            first.afterTestClass();
            assertThat(context.isActive()).isTrue();
            publisher.publish("/context_reuse", "std_msgs/msg/String", RosQos.reliableVolatile(10),
                    JsonNodeFactory.instance.objectNode().put("data", "alive"), Duration.ofSeconds(2));
            second.afterTestClass();
            assertThat(context.isActive()).isTrue();

            second.getTestContext().markApplicationContextDirty(DirtiesContext.HierarchyMode.CURRENT_LEVEL);
            assertThat(context.isActive()).isFalse();
            assertThatThrownBy(() -> publisher.publish("/context_reuse", "std_msgs/msg/String",
                    RosQos.reliableVolatile(10), JsonNodeFactory.instance.objectNode(), Duration.ofSeconds(2)))
                    .hasMessage("ROS transport: CLOSED");
        } finally {
            try {
                first.getTestContext().markApplicationContextDirty(DirtiesContext.HierarchyMode.CURRENT_LEVEL);
            } finally {
                try {
                    second.getTestContext().markApplicationContextDirty(DirtiesContext.HierarchyMode.CURRENT_LEVEL);
                } finally {
                    Files.deleteIfExists(ProtocolFixture.python);
                }
            }
        }
    }

    static class ProtocolFixture {

        static Path python;

        @DynamicPropertySource
        static void properties(final DynamicPropertyRegistry registry) {
            registry.add("forge-it.modules.ros.python-command", () -> python.toString());
        }
    }

    @E2E
    static class FirstRos extends ProtocolFixture {
        RosOnlySupport forgeIt;
    }

    @E2E
    static class SecondRos extends ProtocolFixture {
        RosOnlySupport forgeIt;
    }
}
