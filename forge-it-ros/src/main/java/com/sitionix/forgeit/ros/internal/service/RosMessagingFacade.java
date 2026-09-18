package com.sitionix.forgeit.ros.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sitionix.forgeit.ros.api.*;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.transport.*;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongSupplier;

/** Owns fixture/assertion semantics; transport exposes only a bounded stream. */
public final class RosMessagingFacade implements RosMessaging {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final RosTransport transport;
    private final Environment environment;
    private final Duration consumeTimeout;
    private final Duration publishTimeout;
    private final Function<String, String> fixtures;
    private final LongSupplier nanoTime;

    public RosMessagingFacade(RosTransport transport, Environment environment, RosProperties properties,
                              Function<String, String> fixtures) {
        this(transport, environment, properties, fixtures, System::nanoTime);
    }
    RosMessagingFacade(RosTransport transport, Environment environment, RosProperties properties,
                       Function<String, String> fixtures, LongSupplier nanoTime) {
        this.transport = transport;
        this.environment = environment;
        this.consumeTimeout = positive(properties.getDefaultConsumeTimeout());
        this.publishTimeout = positive(properties.getStartupTimeout());
        this.fixtures = fixtures;
        this.nanoTime = nanoTime;
    }
    @Override public RosPublishBuilder publish(RosTopicContract contract) {
        return new Publisher(Objects.requireNonNull(contract, "ROS contract is required"));
    }
    @Override public RosConsumeBuilder consume(RosTopicContract contract) {
        return new Consumer(Objects.requireNonNull(contract, "ROS contract is required"));
    }
    private static Duration positive(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("ROS wait duration must be non-null and positive");
        }
        try { duration.toNanos(); } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("ROS wait duration exceeds the monotonic clock range");
        }
        return duration;
    }
    private JsonNode fixture(String folder, String file) {
        if (file == null || file.isBlank() || file.contains("..") || !file.matches("[A-Za-z0-9_/-]+\\.json")) {
            throw new IllegalArgumentException("ROS fixture must be a relative JSON resource name");
        }
        try {
            JsonNode result = JSON.readTree(fixtures.apply(folder + "/" + file));
            if (result == null || !result.isObject()) throw new IllegalArgumentException();
            return result;
        } catch (RuntimeException | java.io.IOException ex) {
            throw new IllegalArgumentException("ROS fixture could not be loaded as a JSON object");
        }
    }
    private final class Publisher implements RosPublishBuilder {
        private final RosTopicContract contract;
        private String message;
        Publisher(RosTopicContract contract) { this.contract = contract; }
        @Override public RosPublishBuilder message(String fixture) { message = fixture; return this; }
        @Override public void publish() { send("/ros/publish", message); }
        @Override public void publishDefault() {
            send(message == null ? "/ros/default/publish" : "/ros/publish",
                    message == null ? contract.defaultPublishMessage() : message);
        }
        private void send(String folder, String name) {
            JsonNode payload = fixture(folder, name);
            transport.publish(contract.resolveTopic(environment), contract.messageType(), contract.qos(), payload, publishTimeout);
        }
    }
    private final class Consumer implements RosConsumeBuilder {
        private final RosTopicContract contract;
        private Duration firstTimeout = consumeTimeout;
        private Duration assertionTimeout;
        private String[] ignored = new String[0];
        Consumer(RosTopicContract contract) { this.contract = contract; }
        @Override public RosConsumeBuilder ignoreFields(String... fields) {
            if (fields == null || Arrays.stream(fields).anyMatch(field -> field == null || field.isBlank())) {
                throw new IllegalArgumentException("ROS ignored field names must be nonblank");
            }
            ignored = fields.clone(); return this;
        }
        @Override public RosConsumeBuilder await(Duration duration) { firstTimeout = positive(duration); return this; }
        @Override public RosConsumeBuilder waitUntilAsserted(Duration duration) { assertionTimeout = positive(duration); return this; }
        @Override public void assertMessage() { check(fixture("/ros/default/expected", contract.defaultExpectedMessage())); }
        @Override public void assertMessage(String name) { check(fixture("/ros/expected", name)); }
        private void check(JsonNode expected) {
            removeIgnored(expected, ignored);
            Duration timeout = assertionTimeout == null ? firstTimeout : assertionTimeout;
            long budget = timeout.toNanos();
            String topic = contract.resolveTopic(environment);
            long started = nanoTime.getAsLong();
            int received = 0;
            String last = "no message received";
            try (RosSubscription subscription = transport.subscribe(topic, contract.messageType(), contract.qos(), remaining(started, budget))) {
                while (true) {
                    Duration remaining = remaining(started, budget);
                    if (remaining.isZero()) throw timeout(timeout, received, last);
                    JsonNode message = subscription.next(remaining);
                    if (message == null) throw timeout(timeout, received, last);
                    received++;
                    // A late response cannot extend the overall assertion deadline.
                    if (remaining(started, budget).isZero()) throw timeout(timeout, received, last);
                    String mismatch = compare(expected, message, ignored);
                    if (mismatch == null) return;
                    last = mismatch;
                    if (assertionTimeout == null) {
                        throw new AssertionError("ROS first message assertion failed for " + contract.topicTemplate() + ": " + last);
                    }
                }
            } catch (IllegalStateException ex) {
                if (remaining(started, budget).isZero()) throw timeout(timeout, received, last);
                throw ex;
            }
        }
        private Duration remaining(long started, long budget) {
            return Duration.ofNanos(Math.max(0, budget - (nanoTime.getAsLong() - started)));
        }
        private AssertionError timeout(Duration duration, int received, String last) {
            return new AssertionError("ROS assertion timeout: topic=" + contract.topicTemplate() + "; wait=" + duration
                    + "; received=" + received + "; last assertion: " + last);
        }
    }
    private static String compare(JsonNode expected, JsonNode actual, String[] ignored) {
        JsonNode copy = actual.deepCopy();
        removeIgnored(copy, ignored);
        try {
            var result = JSONCompare.compareJSON(expected.toString(), copy.toString(), JSONCompareMode.LENIENT);
            if (result.passed()) return null;
            // JSONAssert's normal failure text includes actual values: keep only structural counts.
            return "JSON mismatch (different=" + result.getFieldFailures().size() + ", missing="
                    + result.getFieldMissing().size() + ", unexpected=" + result.getFieldUnexpected().size() + ")";
        } catch (Exception ex) { throw new IllegalStateException("ROS message could not be compared as JSON"); }
    }
    private static void removeIgnored(JsonNode node, String[] fields) {
        if (node.isObject()) {
            for (String field : fields) ((ObjectNode) node).remove(field);
            node.elements().forEachRemaining(child -> removeIgnored(child, fields));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> removeIgnored(child, fields));
        }
    }
}
