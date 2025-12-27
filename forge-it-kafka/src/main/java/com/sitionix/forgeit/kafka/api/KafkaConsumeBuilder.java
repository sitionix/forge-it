package com.sitionix.forgeit.kafka.api;

import java.time.Duration;
import java.util.function.Consumer;

public interface KafkaConsumeBuilder<T> {

    KafkaConsumeBuilder<T> await(Duration timeout);

    KafkaConsumeBuilder<T> assertPayload(Consumer<T> mutator);

    KafkaConsumeBuilder<T> assertPayload(String payloadName);

    KafkaConsumeBuilder<T> assertPayload(String payloadName, Consumer<T> mutator);

    KafkaConsumeBuilder<T> assertMetadata(Consumer<T> mutator);

    KafkaConsumeBuilder<T> assertMetadata(String metadataName);

    KafkaConsumeBuilder<T> assertMetadata(String metadataName, Consumer<T> mutator);

    KafkaConsumeBuilder<T> assertEnvelope(Consumer<T> mutator);
}
