package com.sitionix.forgeit.ros.internal.service;

import com.fasterxml.jackson.databind.*;
import com.sitionix.forgeit.ros.api.*;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.transport.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class RosMessagingTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final RosTopicContract TOPIC = RosTopicContract.builder().topicFromProperty("test.topic")
            .messageType("std_msgs/msg/String").defaultPublishMessage("send.json").defaultExpectedMessage("ready.json").build();
    private static JsonNode json(String value) {
        try { return MAPPER.readTree(value); } catch (Exception ex) { throw new AssertionError(ex); }
    }
    private final AtomicLong clock = new AtomicLong();
    private final FakeTransport transport = new FakeTransport();
    private final List<String> fixtures = new ArrayList<>();
    private RosMessagingFacade messaging() {
        return new RosMessagingFacade(transport, new MockEnvironment().withProperty("test.topic", "/private_runtime_topic"),
                new RosProperties(), path -> { fixtures.add(path); return "{\"data\":\"READY\"}"; }, clock::get);
    }
    @Test void basicConsumerAssertsOnlyFirstMessageAndStops() {
        transport.receive = remaining -> json("{\"data\":\"READY\"}");
        messaging().consume(TOPIC).assertMessage();
        assertEquals(1, transport.received);
        assertTrue(transport.closed);
        assertEquals(List.of("/ros/default/expected/ready.json"), fixtures);
    }
    @Test void basicMismatchFailsImmediatelyWithoutReadingLaterMatchOrLeakingValues() {
        transport.receive = remaining -> transport.received == 1 ? json("{\"data\":\"secret-not-ready\"}") : json("{\"data\":\"READY\"}");
        var failure = assertThrows(AssertionError.class, () -> messaging().consume(TOPIC).assertMessage("ready.json"));
        assertEquals(1, transport.received);
        assertTrue(transport.closed);
        assertTrue(failure.getMessage().contains("JSON mismatch"));
        assertFalse(failure.toString().contains("secret"));
        assertFalse(failure.toString().contains("private_runtime"));
        assertNull(failure.getCause());
    }
    @Test void streamingChecksEveryNewMessageUntilLaterMatch() {
        transport.receive = remaining -> json(transport.received < 4 ? "{\"data\":\"STARTING\"}" : "{\"data\":\"READY\"}");
        messaging().consume(TOPIC).waitUntilAsserted(Duration.ofSeconds(10)).assertMessage("ready.json");
        assertEquals(4, transport.received);
        assertTrue(transport.closed);
    }
    @Test void streamingDeadlineIsNotResetAndHasSafeDiagnostics() {
        transport.receive = remaining -> {
            clock.addAndGet(Duration.ofSeconds(3).toNanos());
            return json("{\"data\":\"secret-never-ready\"}");
        };
        var failure = assertThrows(AssertionError.class, () -> messaging().consume(TOPIC)
                .waitUntilAsserted(Duration.ofSeconds(10)).assertMessage("ready.json"));
        assertEquals(List.of(Duration.ofSeconds(10), Duration.ofSeconds(7), Duration.ofSeconds(4), Duration.ofSeconds(1)), transport.waits);
        assertTrue(failure.getMessage().contains("${test.topic}"));
        assertTrue(failure.getMessage().contains("PT10S"));
        assertTrue(failure.getMessage().contains("received=4"));
        assertTrue(failure.getMessage().contains("JSON mismatch"));
        assertFalse(failure.toString().contains("secret"));
        assertFalse(failure.toString().contains("private_runtime"));
    }
    @Test void noMessageTimeoutIncludesZeroCountAndAwaitBudget() {
        transport.receive = remaining -> { clock.addAndGet(remaining.toNanos()); return null; };
        var failure = assertThrows(AssertionError.class, () -> messaging().consume(TOPIC).await(Duration.ofMillis(250)).assertMessage());
        assertTrue(failure.getMessage().contains("PT0.25S"));
        assertTrue(failure.getMessage().contains("received=0"));
        assertTrue(failure.getMessage().contains("no message received"));
    }
    @Test void ignoreFieldsAppliesIndependentlyToStreamingMessagesIncludingNestedFields() {
        RosMessagingFacade facade = new RosMessagingFacade(transport, new MockEnvironment().withProperty("test.topic", "/topic"),
                new RosProperties(), path -> "{\"data\":\"READY\",\"timestamp\":0,\"nested\":{\"sequence\":0}}", clock::get);
        transport.receive = remaining -> json(transport.received == 1
                ? "{\"data\":\"WAIT\",\"timestamp\":1,\"nested\":{\"sequence\":5}}"
                : "{\"data\":\"READY\",\"timestamp\":2,\"nested\":{\"sequence\":6}}");
        facade.consume(TOPIC).waitUntilAsserted(Duration.ofSeconds(1)).ignoreFields("timestamp", "sequence").assertMessage();
        assertEquals(2, transport.received);
    }
    @Test void publisherUsesExplicitAndDefaultFixturePaths() {
        messaging().publish(TOPIC).message("custom.json").publish();
        messaging().publish(TOPIC).publishDefault();
        assertEquals(List.of("/ros/publish/custom.json", "/ros/default/publish/send.json"), fixtures);
        assertEquals(2, transport.published.size());
        assertEquals("READY", transport.published.getFirst().get("data").asText());
    }
    @Test void invalidDurationAndMissingFixtureFailBeforeSubscription() {
        for (Duration timeout : new Duration[]{null, Duration.ZERO, Duration.ofMillis(-1)}) {
            assertThrows(IllegalArgumentException.class, () -> messaging().consume(TOPIC).await(timeout));
            assertThrows(IllegalArgumentException.class, () -> messaging().consume(TOPIC).waitUntilAsserted(timeout));
        }
        assertThrows(IllegalArgumentException.class, () -> messaging().consume(TOPIC).assertMessage("../secret.json"));
        assertThrows(IllegalArgumentException.class, () -> messaging().publish(TOPIC).publish());
        assertEquals(0, transport.subscribed);
    }
    @Test void subscriptionSetupConsumesSameDeadline() {
        transport.setup = () -> clock.addAndGet(Duration.ofSeconds(4).toNanos());
        transport.receive = remaining -> json("{\"data\":\"READY\"}");
        messaging().consume(TOPIC).waitUntilAsserted(Duration.ofSeconds(5)).assertMessage();
        assertEquals(List.of(Duration.ofSeconds(1)), transport.waits);
    }
    static final class FakeTransport implements RosTransport {
        Function<Duration, JsonNode> receive;
        Runnable setup = () -> {};
        int received, subscribed;
        boolean closed;
        List<Duration> waits = new ArrayList<>();
        List<JsonNode> published = new ArrayList<>();
        public RosSubscription subscribe(String topic, String type, RosQos qos, Duration timeout) {
            subscribed++; setup.run();
            return new RosSubscription() {
                public JsonNode next(Duration remaining) { received++; waits.add(remaining); return receive.apply(remaining); }
                public void close() { closed = true; }
            };
        }
        public void publish(String topic, String type, RosQos qos, JsonNode message, Duration timeout) { published.add(message); }
        public void close() { }
    }
}
