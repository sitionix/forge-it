package com.sitionix.forgeit.consumer.ros;

import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.ros.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real DDS tests; source a ROS installation before invoking Maven. */
@E2E
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfSystemProperty(named = "forgeit.ros.real", matches = "true")
class RosRealSelfTest {
    @Autowired RosOnlySupport forgeIt;
    private RosTopicContract topic() {
        return RosTopicContract.builder().topic("/forgeit_self_" + UUID.randomUUID().toString().replace("-", ""))
                .messageType("std_msgs/msg/String").qos(RosQos.reliableVolatile(10))
                .defaultExpectedMessage("ready.json").defaultPublishMessage("ready.json").build();
    }
    @Test void unknownMessageTypeFailsExplicitly() {
        var unknown = RosTopicContract.builder().topic("/forgeit_unknown_type")
                .messageType("forgeit_nonexistent_package/msg/Missing").defaultExpectedMessage("ready.json").build();
        var error = assertThrows(IllegalStateException.class, () -> forgeIt.ros().consume(unknown).assertMessage());
        assertEquals("ROS transport: MESSAGE_TYPE_UNAVAILABLE", error.getMessage());
        assertNull(error.getCause());
    }
    @Test
    void assertsFirstRealMessage() {
        final RosTopicContract contract = this.topic();
        this.forgeIt.ros()
                .publish(contract)
                .publishAndVerify(contract);
    }
    @Test void streamingPassesOnLaterRealMessage() throws Exception {
        var topic = topic();
        var ros = forgeIt.ros();
        try (var worker = Executors.newVirtualThreadPerTaskExecutor()) {
            var consumer = worker.submit(() -> ros.consume(topic).waitUntilAsserted(Duration.ofSeconds(10)).assertMessage());
            ros.publish(topic).message("starting.json").publish();
            assertFalse(consumer.isDone(), "STARTING must not satisfy FUSION_READY");
            ros.publish(topic).publishDefault();
            consumer.get(12, TimeUnit.SECONDS);
        }
    }
    @Test void basicConsumerRejectsFirstRealMismatch() throws Exception {
        var topic = topic();
        var ros = forgeIt.ros();
        try (var worker = Executors.newVirtualThreadPerTaskExecutor()) {
            var consumer = worker.submit(() -> assertThrows(AssertionError.class,
                    () -> ros.consume(topic).await(Duration.ofSeconds(10)).assertMessage()));
            ros.publish(topic).message("starting.json").publish();
            assertTrue(consumer.get(12, TimeUnit.SECONDS).getMessage().contains("first message assertion failed"));
        }
    }
}
