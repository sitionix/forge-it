package com.sitionix.forgeit.ros.internal.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.ros.api.RosConsumeBuilder;
import com.sitionix.forgeit.ros.api.RosQos;
import com.sitionix.forgeit.ros.api.RosTopicContract;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import com.sitionix.forgeit.ros.internal.port.RosSubscription;
import com.sitionix.forgeit.ros.internal.loader.RosLoader;
import com.sitionix.forgeit.ros.internal.service.RosMessagingFacade;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private RosLoader loader() {
        return new RosLoader(path -> {
            fixtures.add(path);
            return "{\"data\":\"READY\"}";
        });
    }
    private RosMessagingFacade messaging() {
        return new RosMessagingFacade(loader(), new MockEnvironment().withProperty("test.topic", "/private_runtime_topic"),
                new RosProperties(), transport, transport);
    }
    private DefaultRosConsumeBuilder consume() {
        return new DefaultRosConsumeBuilder(TOPIC, loader(),
                new MockEnvironment().withProperty("test.topic", "/private_runtime_topic"),
                new RosProperties(), transport, clock::get);
    }
    @Test
    void basicConsumerAssertsOnlyFirstMessageAndStops() {
        transport.receive = remaining -> json("{\"data\":\"READY\"}");
        consume().assertMessage();
        assertEquals(1, transport.received);
        assertTrue(transport.closed);
        assertEquals(List.of("/ros/default/expected/ready.json"), fixtures);
    }
    @Test
    void basicMismatchFailsImmediatelyWithoutReadingLaterMatchOrLeakingValues() {
        transport.receive = remaining -> transport.received == 1 ? json("{\"data\":\"secret-not-ready\"}") : json("{\"data\":\"READY\"}");
        var failure = assertThrows(AssertionError.class, () -> consume().assertMessage("ready.json"));
        assertEquals(1, transport.received);
        assertTrue(transport.closed);
        assertTrue(failure.getMessage().contains("JSON mismatch"));
        assertFalse(failure.toString().contains("secret"));
        assertFalse(failure.toString().contains("private_runtime"));
        assertNull(failure.getCause());
    }
    @Test
    void streamingChecksEveryNewMessageUntilLaterMatch() {
        transport.receive = remaining -> json(transport.received < 4 ? "{\"data\":\"STARTING\"}" : "{\"data\":\"READY\"}");
        consume().waitUntilAsserted(Duration.ofSeconds(10)).assertMessage("ready.json");
        assertEquals(4, transport.received);
        assertTrue(transport.closed);
    }
    @Test
    void streamingDeadlineIsNotResetAndHasSafeDiagnostics() {
        transport.receive = remaining -> {
            clock.addAndGet(Duration.ofSeconds(3).toNanos());
            return json("{\"data\":\"secret-never-ready\"}");
        };
        var failure = assertThrows(AssertionError.class, () -> consume()
                .waitUntilAsserted(Duration.ofSeconds(10)).assertMessage("ready.json"));
        assertEquals(List.of(Duration.ofSeconds(10), Duration.ofSeconds(7), Duration.ofSeconds(4), Duration.ofSeconds(1)), transport.waits);
        assertTrue(failure.getMessage().contains("${test.topic}"));
        assertTrue(failure.getMessage().contains("PT10S"));
        assertTrue(failure.getMessage().contains("received=4"));
        assertTrue(failure.getMessage().contains("JSON mismatch"));
        assertFalse(failure.toString().contains("secret"));
        assertFalse(failure.toString().contains("private_runtime"));
    }
    @Test
    void noMessageTimeoutIncludesZeroCountAndAwaitBudget() {
        transport.receive = remaining -> { clock.addAndGet(remaining.toNanos()); return null; };
        var failure = assertThrows(AssertionError.class, () -> consume().await(Duration.ofMillis(250)).assertMessage());
        assertTrue(failure.getMessage().contains("PT0.25S"));
        assertTrue(failure.getMessage().contains("received=0"));
        assertTrue(failure.getMessage().contains("no message received"));
    }
    @Test
    void ignoreFieldsAppliesIndependentlyToStreamingMessagesIncludingNestedFields() {
        final RosConsumeBuilder consumer = new DefaultRosConsumeBuilder(TOPIC,
                new RosLoader(path -> "{\"data\":\"READY\",\"timestamp\":0,\"nested\":{\"sequence\":0}}"),
                new MockEnvironment().withProperty("test.topic", "/topic"),
                new RosProperties(), transport, clock::get);
        transport.receive = remaining -> json(transport.received == 1
                ? "{\"data\":\"WAIT\",\"timestamp\":1,\"nested\":{\"sequence\":5}}"
                : "{\"data\":\"READY\",\"timestamp\":2,\"nested\":{\"sequence\":6}}");
        consumer.waitUntilAsserted(Duration.ofSeconds(1)).ignoreFields("timestamp", "sequence").assertMessage();
        assertEquals(2, transport.received);
    }
    @Test
    void publisherUsesExplicitAndDefaultFixturePaths() {
        messaging().publish(TOPIC).message("custom.json").publish();
        messaging().publish(TOPIC).publishDefault();
        assertEquals(List.of("/ros/publish/custom.json", "/ros/default/publish/send.json"), fixtures);
        assertEquals(2, transport.published.size());
        assertEquals("READY", transport.published.getFirst().get("data").asText());
    }
    @Test
    void invalidDurationAndMissingFixtureFailBeforeSubscription() {
        for (Duration timeout : new Duration[]{null, Duration.ZERO, Duration.ofMillis(-1)}) {
            assertThrows(IllegalArgumentException.class, () -> consume().await(timeout));
            assertThrows(IllegalArgumentException.class, () -> consume().waitUntilAsserted(timeout));
        }
        assertThrows(IllegalArgumentException.class, () -> consume().assertMessage("../secret.json"));
        assertThrows(IllegalArgumentException.class, () -> messaging().publish(TOPIC).publish());
        assertEquals(0, transport.subscribed);
    }
    @Test
    void subscriptionSetupConsumesSameDeadline() {
        transport.setup = () -> clock.addAndGet(Duration.ofSeconds(4).toNanos());
        transport.receive = remaining -> json("{\"data\":\"READY\"}");
        consume().waitUntilAsserted(Duration.ofSeconds(5)).assertMessage();
        assertEquals(List.of(Duration.ofSeconds(1)), transport.waits);
    }
    @Test
    void producerAndFeedbackContractsResolveSeparateTopicsThroughTheFacade() {
        final RosTopicContract producer = RosTopicContract.builder()
                .topic("/command")
                .messageType("std_msgs/msg/String")
                .defaultPublishMessage("send.json")
                .build();
        final RosTopicContract feedback = RosTopicContract.builder()
                .topic("/feedback")
                .messageType("std_msgs/msg/String")
                .defaultExpectedMessage("ready.json")
                .build();
        transport.receive = remaining -> json("{\"data\":\"READY\"}");
        final RosMessagingFacade facade = messaging();

        facade.publish(producer).publishDefault();
        facade.consume(feedback).assertMessage();

        assertEquals("/command", transport.publishedTopic);
        assertEquals("/feedback", transport.subscribedTopic);
        assertEquals(List.of("/ros/default/publish/send.json", "/ros/default/expected/ready.json"), fixtures);
        assertTrue(transport.closed);
    }

    @Test
    void publishAndVerifySubscribesBeforePublishingAndAssertsDefaultFixture() {
        this.transport.receive = remaining -> this.transport.published.getFirst();

        this.messaging().publish(TOPIC).publishAndVerify(TOPIC);

        assertEquals(List.of("subscribe", "publish", "receive", "close"), this.transport.events);
        assertEquals(List.of("/ros/default/publish/send.json", "/ros/default/expected/ready.json"), this.fixtures);
    }

    @Test
    void publishAndVerifySupportsSeparateFeedbackContractAndExplicitPublishFixture() {
        final RosTopicContract feedback = RosTopicContract.builder()
                .topic("/feedback")
                .messageType("std_msgs/msg/String")
                .defaultExpectedMessage("ready.json")
                .build();
        this.transport.receive = remaining -> this.transport.published.getFirst();

        this.messaging().publish(TOPIC).message("custom.json").publishAndVerify(feedback);

        assertEquals("/private_runtime_topic", this.transport.publishedTopic);
        assertEquals("/feedback", this.transport.subscribedTopic);
        assertEquals(List.of("/ros/publish/custom.json", "/ros/default/expected/ready.json"), this.fixtures);
        assertTrue(this.transport.closed);
    }

    @Test
    void publishAndVerifyFailsOnFirstMismatchAndClosesSubscription() {
        this.transport.receive = remaining -> json("{\"data\":\"private-mismatch\"}");

        final AssertionError failure = assertThrows(AssertionError.class,
                () -> this.messaging().publish(TOPIC).publishAndVerify(TOPIC));

        assertEquals(1, this.transport.received);
        assertTrue(this.transport.closed);
        assertTrue(failure.getMessage().contains("first message assertion failed"));
        assertFalse(failure.getMessage().contains("private-mismatch"));
    }

    @Test
    void publishAndVerifyClosesSubscriptionWhenPublicationFails() {
        this.transport.publication = () -> { throw new IllegalStateException("publication failed"); };

        assertThrows(IllegalStateException.class,
                () -> this.messaging().publish(TOPIC).publishAndVerify(TOPIC));

        assertTrue(this.transport.closed);
        assertEquals(0, this.transport.received);
    }

    @Test
    void publicationConsumesTheSameOverallAssertionDeadline() {
        this.transport.setup = () -> this.clock.addAndGet(Duration.ofSeconds(2).toNanos());
        this.transport.receive = remaining -> json("{\"data\":\"READY\"}");
        final List<Duration> publishBudgets = new ArrayList<>();

        this.consume().assertMessage(remaining -> {
            publishBudgets.add(remaining);
            this.clock.addAndGet(Duration.ofSeconds(2).toNanos());
        });

        assertEquals(List.of(Duration.ofSeconds(3)), publishBudgets);
        assertEquals(List.of(Duration.ofSeconds(1)), this.transport.waits);
        assertTrue(this.transport.closed);
    }

    @Test
    void publicationExhaustingDeadlineCannotPassWithALateMessage() {
        this.transport.receive = remaining -> json("{\"data\":\"READY\"}");

        final AssertionError failure = assertThrows(AssertionError.class,
                () -> this.consume().assertMessage(remaining -> this.clock.addAndGet(remaining.toNanos())));

        assertTrue(failure.getMessage().contains("received=0"));
        assertEquals(0, this.transport.received);
        assertTrue(this.transport.closed);
    }

    @Test
    void publishAndVerifyValidatesFixturesAndFeedbackBeforePublishing() {
        assertThrows(NullPointerException.class,
                () -> this.messaging().publish(TOPIC).publishAndVerify(null));
        assertThrows(IllegalArgumentException.class,
                () -> this.messaging().publish(TOPIC).message("../secret.json").publishAndVerify(TOPIC));
        final RosTopicContract missingExpected = RosTopicContract.builder()
                .topic("/feedback").messageType("std_msgs/msg/String").build();
        assertThrows(IllegalArgumentException.class,
                () -> this.messaging().publish(TOPIC).publishAndVerify(missingExpected));

        assertEquals(0, this.transport.subscribed);
        assertTrue(this.transport.published.isEmpty());
    }

    static final class FakeTransport implements RosPublisherPort, RosConsumerPort {
        final List<String> events = new ArrayList<>();
        Runnable publication = () -> {};
        String publishedTopic;
        String subscribedTopic;
        Function<Duration, JsonNode> receive;
        Runnable setup = () -> {};
        int received, subscribed;
        boolean closed;
        List<Duration> waits = new ArrayList<>();
        List<JsonNode> published = new ArrayList<>();
        public RosSubscription subscribe(String topic, String type, RosQos qos, Duration timeout) {
            events.add("subscribe");
            subscribedTopic = topic;
            subscribed++;
            setup.run();
            return new RosSubscription() {
                public JsonNode next(Duration remaining) { events.add("receive"); received++; waits.add(remaining); return receive.apply(remaining); }
                public void close() { events.add("close"); closed = true; }
            };
        }
        public void publish(final String topic, final String type, final RosQos qos, final JsonNode message, final Duration timeout) {
            events.add("publish");
            publication.run();
            publishedTopic = topic;
            published.add(message);
        }
        public void close() { }
    }
}
