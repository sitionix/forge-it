package com.sitionix.forgeit.kafka.api;

import java.time.Duration;
import java.util.function.Consumer;

public interface KafkaConsumeBuilder<T> {

    KafkaConsumeBuilder<T> await(Duration timeout);

    void assertPayload(String payloadName);

    void assertPayload(String payloadName, Consumer<T> mutator);

    void assertDefaultPayload(String payloadName);

    void assertDefaultPayload(String payloadName, Consumer<T> mutator);

    void assertDefaultPayload();

    void assertDefaultPayload(Consumer<T> mutator);
}
