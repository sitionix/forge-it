package com.sitionix.forgeit.kafka.api;

import java.util.function.Consumer;

public interface KafkaPublishBuilder<T> {

    KafkaPublishBuilder<T> payload(Consumer<T> mutator);

    KafkaPublishBuilder<T> payload(String payloadName);

    KafkaPublishBuilder<T> payload(String payloadName, Consumer<T> mutator);

    KafkaPublishBuilder<T> metadata(Consumer<T> mutator);

    KafkaPublishBuilder<T> metadata(String metadataName);

    KafkaPublishBuilder<T> metadata(String metadataName, Consumer<T> mutator);

    KafkaPublishBuilder<T> envelope(Consumer<T> mutator);

    KafkaPublishBuilder<T> key(String key);

    void send();
}
