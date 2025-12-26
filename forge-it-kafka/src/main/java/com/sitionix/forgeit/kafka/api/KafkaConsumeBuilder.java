package com.sitionix.forgeit.kafka.api;

import java.time.Duration;
import java.util.function.Consumer;

public interface KafkaConsumeBuilder<T> {

    KafkaConsumeBuilder<T> await(Duration timeout);

    void expectPayload(String payloadName);

    void expectPayload(String payloadName, Consumer<T> mutator);

    void defaultExpectPayload(String payloadName);

    void defaultExpectPayload(String payloadName, Consumer<T> mutator);

    void defaultExpectPayload();

    void defaultExpectPayload(Consumer<T> mutator);
}
