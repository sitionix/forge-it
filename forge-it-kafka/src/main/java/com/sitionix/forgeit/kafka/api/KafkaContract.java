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
    private final String defaultPayloadName;
    private final String defaultExpectedPayloadName;

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<T> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName) {
        return new KafkaContract<>(topic, payloadType, defaultPayloadName, defaultExpectedPayloadName);
    }

    public static <T> KafkaProducerContractBuilder<T> producerContract(final Class<T> payloadType) {
        return KafkaProducerContractBuilder.forPayload(payloadType);
    }

    public static <T> KafkaConsumerContractBuilder<T> consumerContract(final Class<T> payloadType) {
        return KafkaConsumerContractBuilder.forPayload(payloadType);
    }
}
