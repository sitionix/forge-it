package com.sitionix.forgeit.consumer.ros;

import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.ros.api.*;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.*;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

/** Tests generated support/context wiring using an explicit protocol fixture, without ROS installed. */
@E2E
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RosContextSelfTest {
    @Autowired RosOnlySupport forgeIt;
    @Autowired ApplicationContext context;
    static Path python;
    static final RosTopicContract STATUS = RosTopicContract.builder().topicFromProperty("test.ros.topic")
            .messageType("std_msgs/msg/String").defaultExpectedMessage("ready.json").defaultPublishMessage("ready.json").build();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        python = Files.createTempFile("forge-ros-context-", ".py");
        try (var resource = RosContextSelfTest.class.getResourceAsStream("/ros-context-adapter.py")) {
            Files.copy(resource, python, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!python.toFile().setExecutable(true, true)) throw new IllegalStateException("Cannot mark protocol fixture executable");
        registry.add("forge-it.modules.ros.python-command", () -> python.toString());
        registry.add("test.ros.topic", () -> "/context_self_test");
    }
    @AfterAll static void cleanup() throws Exception { Files.deleteIfExists(python); }
    @Test void installsOnlyRosAndReusesOneTransport() {
        assertThat(forgeIt.getClass().getSimpleName()).isEqualTo("RosOnlySupportImpl");
        assertThat(forgeIt.ros()).isSameAs(forgeIt.ros());
        assertThat(context.getBeansOfType(RosPublisherPort.class)).hasSize(1);
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(org.springframework.test.web.servlet.MockMvc.class)).isEmpty();
        for (String name : context.getBeanDefinitionNames()) {
            assertThat(name.toLowerCase()).doesNotContain("container", "wiremock", "mockmvc", "kafkatemplate");
        }
        forgeIt.ros().publish(STATUS).publishDefault();
    }
    @Test void firstMessageAndStreamingAreObservablyDifferent() {
        assertThatThrownBy(() -> forgeIt.ros().consume(STATUS).assertMessage()).isInstanceOf(AssertionError.class)
                .hasMessageContaining("first message assertion failed");
        forgeIt.ros().consume(STATUS).waitUntilAsserted(Duration.ofSeconds(2)).assertMessage();
    }
}
