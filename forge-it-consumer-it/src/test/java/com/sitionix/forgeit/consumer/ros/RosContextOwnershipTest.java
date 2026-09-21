package com.sitionix.forgeit.consumer.ros;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sitionix.forgeit.consumer.e2e.E2eSupport;
import com.sitionix.forgeit.consumer.kafka.KafkaItSupport;
import com.sitionix.forgeit.core.internal.test.IntegrationTestContextCustomizerFactory;
import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.ros.api.RosQos;
import com.sitionix.forgeit.ros.internal.transport.RosTransport;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RosContextOwnershipTest {
    private final IntegrationTestContextCustomizerFactory factory = new IntegrationTestContextCustomizerFactory();

    @Test void rosE2eContextsBelongToOneClassAndRemainReusableWithinThatClass() {
        var first = factory.createContextCustomizer(FirstRos.class, List.of());
        assertThat(first).isEqualTo(factory.createContextCustomizer(FirstRos.class, List.of()));
        assertThat(first).isNotEqualTo(factory.createContextCustomizer(SecondRos.class, List.of()));
    }

    @Test void rosIntegrationContextsBelongToOneClass() {
        var first = factory.createContextCustomizer(FirstRosIt.class, List.of());
        assertThat(first).isEqualTo(factory.createContextCustomizer(FirstRosIt.class, List.of()));
        assertThat(first).isNotEqualTo(factory.createContextCustomizer(SecondRosIt.class, List.of()));
    }

    @Test void httpContextsStillShareAcrossClasses() {
        assertThat(factory.createContextCustomizer(FirstHttp.class, List.of()))
                .isEqualTo(factory.createContextCustomizer(SecondHttp.class, List.of()));
        assertThat(factory.createContextCustomizer(FirstHttpIt.class, List.of()))
                .isEqualTo(factory.createContextCustomizer(SecondHttpIt.class, List.of()));
    }

    @Test void kafkaContextsStillShareAcrossClasses() {
        assertThat(factory.createContextCustomizer(FirstKafka.class, List.of()))
                .isEqualTo(factory.createContextCustomizer(SecondKafka.class, List.of()));
    }

    @Test void closingOneClassDoesNotCloseAnotherLiveRosContext() throws Exception {
        ProtocolFixture.python = Files.createTempFile("forge-ros-ownership-", ".py");
        var first = new TestContextManager(FirstRos.class);
        var second = new TestContextManager(SecondRos.class);
        try {
            try (var resource = getClass().getResourceAsStream("/ros-context-adapter.py")) {
                Files.copy(resource, ProtocolFixture.python, StandardCopyOption.REPLACE_EXISTING);
            }
            assertThat(ProtocolFixture.python.toFile().setExecutable(true, true)).isTrue();
            var firstContext = (ConfigurableApplicationContext) first.getTestContext().getApplicationContext();
            var secondContext = (ConfigurableApplicationContext) second.getTestContext().getApplicationContext();
            assertThat(firstContext).isNotSameAs(secondContext);
            assertThat(first.getTestContext().getApplicationContext()).isSameAs(firstContext);
            first.afterTestClass();
            assertThat(firstContext.isActive()).isFalse();
            assertThat(secondContext.isActive()).isTrue();
            secondContext.getBean(RosTransport.class).publish("/ownership", "std_msgs/msg/String", RosQos.reliableVolatile(10),
                    JsonNodeFactory.instance.objectNode().put("data", "alive"), Duration.ofSeconds(2));
            second.afterTestClass();
            assertThat(secondContext.isActive()).isFalse();
        } finally {
            try { first.afterTestClass(); }
            finally {
                try { second.afterTestClass(); }
                finally { Files.deleteIfExists(ProtocolFixture.python); }
            }
        }
    }

    static class ProtocolFixture {
        static Path python;
        @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
            registry.add("forge-it.modules.ros.python-command", () -> python.toString());
        }
    }

    @E2E static class FirstRos extends ProtocolFixture { RosOnlySupport forgeIt; }
    @E2E static class SecondRos extends ProtocolFixture { RosOnlySupport forgeIt; }
    @IntegrationTest static class FirstRosIt { RosOnlySupport forgeIt; }
    @IntegrationTest static class SecondRosIt { RosOnlySupport forgeIt; }
    @E2E static class FirstHttp { E2eSupport forgeIt; }
    @E2E static class SecondHttp { E2eSupport forgeIt; }
    @IntegrationTest static class FirstKafka { KafkaItSupport forgeIt; }
    @IntegrationTest static class SecondKafka { KafkaItSupport forgeIt; }
    @IntegrationTest static class FirstHttpIt { E2eSupport forgeIt; }
    @IntegrationTest static class SecondHttpIt { E2eSupport forgeIt; }
}
