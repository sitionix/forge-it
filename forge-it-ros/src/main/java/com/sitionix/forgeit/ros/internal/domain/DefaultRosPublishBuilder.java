package com.sitionix.forgeit.ros.internal.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.sitionix.forgeit.ros.api.RosPublishBuilder;
import com.sitionix.forgeit.ros.api.RosTopicContract;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.loader.RosLoader;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Objects;

public final class DefaultRosPublishBuilder implements RosPublishBuilder {

    private final RosTopicContract contract;
    private final RosLoader rosLoader;
    private final Environment environment;
    private final RosPublisherPort publisherPort;
    private final RosConsumerPort consumerPort;
    private final RosProperties properties;
    private final Duration publishTimeout;
    private String message;
    private Long frequency;

    public DefaultRosPublishBuilder(final RosTopicContract contract, final RosLoader rosLoader,
                                    final Environment environment, final RosProperties properties,
                                    final RosPublisherPort publisherPort, final RosConsumerPort consumerPort) {
        this.contract = Objects.requireNonNull(contract, "ROS contract is required");
        this.rosLoader = rosLoader;
        this.environment = environment;
        this.publisherPort = publisherPort;
        this.consumerPort = consumerPort;
        this.properties = properties;
        this.publishTimeout = positive(properties.getStartupTimeout());
    }

    @Override
    public RosPublishBuilder message(final String fixture) {
        this.message = fixture;
        return this;
    }

    @Override
    public RosPublishBuilder frequency(final long hertz) {
        if (hertz < 1 || hertz > 100) {
            throw new IllegalArgumentException("ROS publish frequency must be between 1 and 100 Hz");
        }
        this.frequency = hertz;
        return this;
    }

    @Override
    public void publish() {
        this.send(this.rosLoader.payload(this.message));
    }

    @Override
    public void publishDefault() {
        this.send(this.defaultPayload());
    }

    @Override
    public void publishAndVerify(final RosTopicContract feedbackContract) {
        Objects.requireNonNull(feedbackContract, "ROS feedback contract is required");
        final JsonNode payload = this.defaultPayload();
        final String topic = this.contract.resolveTopic(this.environment);
        new DefaultRosConsumeBuilder(feedbackContract, this.rosLoader, this.environment,
                    this.properties, this.consumerPort)
                    .assertMessage(remaining -> {
                        final Duration budget = remaining.compareTo(this.publishTimeout) < 0
                                ? remaining : this.publishTimeout;
                        if (this.frequency == null) {
                            this.publisherPort.publish(topic, this.contract.messageType(), this.contract.qos(), payload, budget);
                        } else {
                            this.publisherPort.publishPeriodically(topic, this.contract.messageType(),
                                    this.contract.qos(), payload, budget, this.frequency);
                        }
                    });
    }

    private JsonNode defaultPayload() {
        return this.message == null
                ? this.rosLoader.defaultPayload(this.contract.defaultPublishMessage())
                : this.rosLoader.payload(this.message);
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

    private void send(final JsonNode payload) {
        final String topic = this.contract.resolveTopic(this.environment);
        if (this.frequency == null) {
            this.publisherPort.publish(topic, this.contract.messageType(), this.contract.qos(), payload,
                    this.publishTimeout);
        } else {
            this.publisherPort.publishPeriodically(topic, this.contract.messageType(), this.contract.qos(),
                    payload, this.publishTimeout, this.frequency);
        }
    }
}
