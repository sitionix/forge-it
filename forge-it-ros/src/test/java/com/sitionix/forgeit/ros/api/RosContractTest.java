package com.sitionix.forgeit.ros.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class RosContractTest {
    @Test void validatesContractAndResolvesAgainstEachEnvironment() {
        var contract = RosTopicContract.builder().topicFromProperty("test.topic")
                .messageType("std_msgs/msg/String").qos(RosQos.reliableVolatile(10))
                .defaultPublishMessage("publish.json").defaultExpectedMessage("expected.json").build();
        assertEquals("/one", contract.resolveTopic(new MockEnvironment().withProperty("test.topic", "/one")));
        assertEquals("/two", contract.resolveTopic(new MockEnvironment().withProperty("test.topic", "/two")));
        assertEquals("${test.topic}", contract.topicTemplate());
        assertEquals("publish.json", contract.defaultPublishMessage());
        assertEquals("expected.json", contract.defaultExpectedMessage());
        assertThrows(IllegalArgumentException.class, () -> contract.resolveTopic(new MockEnvironment()));
        assertThrows(IllegalArgumentException.class, () -> contract.resolveTopic(new MockEnvironment().withProperty("test.topic", "${MISSING}")));
        assertThrows(IllegalArgumentException.class, () -> RosTopicContract.builder().messageType("std_msgs/msg/String").build());
        assertThrows(IllegalArgumentException.class, () -> RosTopicContract.builder().topic("/one").topicFromProperty("test.topic").messageType("std_msgs/msg/String").build());
        assertThrows(IllegalArgumentException.class, () -> RosTopicContract.builder().topic("/one").messageType("bad-secret-type").build());
        assertThrows(IllegalArgumentException.class, () -> RosTopicContract.builder().topic("bad secret").messageType("std_msgs/msg/String").build());
    }
    @Test void qosIsImmutableExplicitAndRejectsUnsupportedValues() {
        var qos = RosQos.reliableVolatile(10);
        assertEquals(RosQos.Reliability.RELIABLE, qos.reliability());
        assertEquals(RosQos.Durability.VOLATILE, qos.durability());
        assertEquals(RosQos.History.KEEP_LAST, qos.history());
        assertEquals(10, qos.depth());
        assertThrows(IllegalArgumentException.class, () -> RosQos.reliableVolatile(0));
        assertThrows(IllegalArgumentException.class, () -> new RosQos(null, qos.durability(), qos.history(), 1));
        assertThrows(IllegalArgumentException.class, () -> RosQos.History.valueOf("KEEP_ALL"));
    }
}
