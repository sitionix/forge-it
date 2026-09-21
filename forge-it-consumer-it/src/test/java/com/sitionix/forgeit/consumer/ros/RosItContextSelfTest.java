package com.sitionix.forgeit.consumer.ros;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import java.nio.file.*;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

/** Exercises the existing IT installer with only ROS selected and a protocol fixture. */
@IntegrationTest
@ActiveProfiles("ros-it-self")
@SpringBootTest(classes = RosItContextSelfTest.TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RosItContextSelfTest {
    @Autowired RosOnlySupport forgeIt;
    @Autowired ApplicationContext context;
    static Path python;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        python = Files.createTempFile("forge-ros-it-", ".py");
        try (var resource = RosItContextSelfTest.class.getResourceAsStream("/ros-context-adapter.py")) {
            Files.copy(resource, python, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!python.toFile().setExecutable(true, true)) throw new IllegalStateException("Cannot execute protocol fixture");
        registry.add("forge-it.modules.ros.python-command", () -> python.toString());
        registry.add("test.ros.topic", () -> "/it_self_test");
    }
    @AfterAll static void cleanup() throws Exception { Files.deleteIfExists(python); }
    @Test void onlyRosIsInstalledInItAndGeneratedDslWorks() {
        assertThat(context.getBeansOfType(RosPublisherPort.class)).hasSize(1);
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        assertThat(context.containsBean("wireMockContainerManager")).isFalse();
        assertThat(context.containsBean("kafkaContainerManager")).isFalse();
        assertThat(context.containsBean("mockMvcJournal")).isFalse();
        forgeIt.ros().consume(RosContextSelfTest.STATUS).waitUntilAsserted(Duration.ofSeconds(2)).assertMessage();
    }
    @Configuration(proxyBeanMethods = false)
    @Profile("ros-it-self")
    static class TestApplication {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean RosOnlySupport rosOnlySupport() { return new RosOnlySupportImpl(); }
    }
}
