package com.sitionix.forgeit.kafka.api;

import java.util.function.Consumer;

public interface KafkaPublishBuilder<T> {

    KafkaPublishBuilder<T> payload(String payloadName);

    KafkaPublishBuilder<T> payload(String payloadName, Consumer<T> mutator);

    KafkaPublishBuilder<T> defaultPayload(String payloadName);

    KafkaPublishBuilder<T> defaultPayload(String payloadName, Consumer<T> mutator);

    KafkaPublishBuilder<T> defaultPayload();

    KafkaPublishBuilder<T> defaultPayload(Consumer<T> mutator);

    KafkaPublishBuilder<T> payloadJson(String payloadJson);

    KafkaPublishBuilder<T> mutate(Consumer<T> mutator);

    KafkaPublishBuilder<T> key(String key);

    void send();
}
