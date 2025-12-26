package com.sitionix.forgeit.kafka.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Public contract describing Kafka message metadata and payload type.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaContract<T> {

    private final String topic;
    private final Class<T> payloadType;

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<T> payloadType) {
        return new KafkaContract<>(topic, payloadType);
    }

    public static <T> KafkaContractBuilder<T> builder(final Class<T> payloadType) {
        return KafkaContractBuilder.forPayload(payloadType);
    }
}
