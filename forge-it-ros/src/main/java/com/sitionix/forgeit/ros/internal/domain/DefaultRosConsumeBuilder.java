package com.sitionix.forgeit.ros.internal.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sitionix.forgeit.ros.api.RosConsumeBuilder;
import com.sitionix.forgeit.ros.api.RosTopicContract;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.loader.RosLoader;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import com.sitionix.forgeit.ros.internal.port.RosSubscription;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Consumer;

public final class DefaultRosConsumeBuilder implements RosConsumeBuilder {

    private final RosTopicContract contract;
    private final RosLoader rosLoader;
    private final Environment environment;
    private final RosConsumerPort consumerPort;
    private final LongSupplier nanoTime;
    private Duration firstTimeout;
    private Duration assertionTimeout;
    private String[] ignored = new String[0];

    public DefaultRosConsumeBuilder(final RosTopicContract contract, final RosLoader rosLoader,
                                    final Environment environment, final RosProperties properties,
                                    final RosConsumerPort consumerPort) {
        this(contract, rosLoader, environment, properties, consumerPort, System::nanoTime);
    }

    DefaultRosConsumeBuilder(final RosTopicContract contract, final RosLoader rosLoader,
                             final Environment environment, final RosProperties properties,
                             final RosConsumerPort consumerPort, final LongSupplier nanoTime) {
        this.contract = Objects.requireNonNull(contract, "ROS contract is required");
        this.rosLoader = rosLoader;
        this.environment = environment;
        this.consumerPort = consumerPort;
        this.nanoTime = nanoTime;
        this.firstTimeout = positive(properties.getDefaultConsumeTimeout());
    }

    @Override
    public RosConsumeBuilder ignoreFields(final String... fields) {
        if (fields == null || Arrays.stream(fields).anyMatch(field -> field == null || field.isBlank())) {
            throw new IllegalArgumentException("ROS ignored field names must be nonblank");
        }
        this.ignored = fields.clone();
        return this;
    }

    @Override
    public RosConsumeBuilder await(final Duration duration) {
        this.firstTimeout = positive(duration);
        return this;
    }

    @Override
    public RosConsumeBuilder waitUntilAsserted(final Duration duration) {
        this.assertionTimeout = positive(duration);
        return this;
    }

    @Override
    public void assertMessage() {
        this.check(this.rosLoader.defaultExpectedPayload(this.contract.defaultExpectedMessage()));
    }

    @Override
    public void assertMessage(final String name) {
        this.check(this.rosLoader.expectedPayload(name));
    }

    void assertMessage(final Consumer<Duration> afterSubscription) {
        this.check(this.rosLoader.defaultExpectedPayload(this.contract.defaultExpectedMessage()), afterSubscription);
    }

    private void check(final JsonNode expected) {
        this.check(expected, remaining -> { });
    }

    private void check(final JsonNode expected, final Consumer<Duration> afterSubscription) {
        removeIgnored(expected, this.ignored);
        final Duration timeout = this.assertionTimeout == null ? this.firstTimeout : this.assertionTimeout;
        final long budget = timeout.toNanos();
        final String topic = this.contract.resolveTopic(this.environment);
        final long started = this.nanoTime.getAsLong();
        int received = 0;
        String last = "no message received";
        try (final RosSubscription subscription = this.consumerPort.subscribe(topic, this.contract.messageType(),
                this.contract.qos(), this.remaining(started, budget))) {
            final Duration publishBudget = this.remaining(started, budget);
            if (publishBudget.isZero()) {
                throw this.timeout(timeout, received, last);
            }
            afterSubscription.accept(publishBudget);
            while (true) {
                final Duration remaining = this.remaining(started, budget);
                if (remaining.isZero()) {
                    throw this.timeout(timeout, received, last);
                }
                final JsonNode message = subscription.next(remaining);
                if (message == null) {
                    throw this.timeout(timeout, received, last);
                }
                received++;
                // A late response cannot extend the overall assertion deadline.
                if (this.remaining(started, budget).isZero()) {
                    throw this.timeout(timeout, received, last);
                }
                final String mismatch = compare(expected, message, this.ignored);
                if (mismatch == null) {
                    return;
                }
                last = mismatch;
                if (this.assertionTimeout == null) {
                    throw new AssertionError("ROS first message assertion failed for "
                            + this.contract.topicTemplate() + ": " + last);
                }
            }
        } catch (final IllegalStateException ex) {
            if (this.remaining(started, budget).isZero()) {
                throw this.timeout(timeout, received, last);
            }
            throw ex;
        }
    }

    private Duration remaining(final long started, final long budget) {
        return Duration.ofNanos(Math.max(0, budget - (this.nanoTime.getAsLong() - started)));
    }

    private AssertionError timeout(final Duration duration, final int received, final String last) {
        return new AssertionError("ROS assertion timeout: topic=" + this.contract.topicTemplate() + "; wait=" + duration
                + "; received=" + received + "; last assertion: " + last);
    }

    private static Duration positive(final Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("ROS wait duration must be non-null and positive");
        }
        try {
            duration.toNanos();
        } catch (final ArithmeticException ex) {
            throw new IllegalArgumentException("ROS wait duration exceeds the monotonic clock range");
        }
        return duration;
    }

    private static String compare(final JsonNode expected, final JsonNode actual, final String[] ignored) {
        final JsonNode copy = actual.deepCopy();
        removeIgnored(copy, ignored);
        try {
            final JSONCompareResult result = JSONCompare.compareJSON(expected.toString(), copy.toString(), JSONCompareMode.LENIENT);
            if (result.passed()) {
                return null;
            }
            // JSONAssert's normal failure text includes actual values: keep only structural counts.
            return "JSON mismatch (different=" + result.getFieldFailures().size() + ", missing="
                    + result.getFieldMissing().size() + ", unexpected=" + result.getFieldUnexpected().size() + ")";
        } catch (final Exception ex) {
            throw new IllegalStateException("ROS message could not be compared as JSON");
        }
    }

    private static void removeIgnored(final JsonNode node, final String[] fields) {
        if (node.isObject()) {
            for (final String field : fields) {
                ((ObjectNode) node).remove(field);
            }
            node.elements().forEachRemaining(child -> removeIgnored(child, fields));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> removeIgnored(child, fields));
        }
    }
}
