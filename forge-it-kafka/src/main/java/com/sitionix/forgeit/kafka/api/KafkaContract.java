package com.sitionix.forgeit.kafka.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Public contract describing Kafka message metadata, payload type, and optional envelope details.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaContract<T> {

    private final String topic;
    private final Class<T> rootType;
    private final Class<?> payloadType;
    private final String defaultPayloadName;
    private final String defaultExpectedPayloadName;
    private final String defaultEnvelopeName;
    private final Class<?> envelopeType;
    private final String defaultMetadataName;
    private final Class<?> metadataType;

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<T> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName) {
        return createContract(topic,
                payloadType,
                payloadType,
                defaultPayloadName,
                defaultExpectedPayloadName,
                null,
                null,
                null,
                null);
    }

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<?> rootType,
                                                      final Class<?> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName,
                                                      final String defaultEnvelopeName,
                                                      final Class<?> envelopeType) {
        return createContract(topic,
                rootType,
                payloadType,
                defaultPayloadName,
                defaultExpectedPayloadName,
                defaultEnvelopeName,
                envelopeType,
                null,
                null);
    }

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<?> rootType,
                                                      final Class<?> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName,
                                                      final String defaultEnvelopeName,
                                                      final Class<?> envelopeType,
                                                      final String defaultMetadataName,
                                                      final Class<?> metadataType) {
        return new KafkaContract<>(topic,
                castType(rootType),
                payloadType,
                defaultPayloadName,
                defaultExpectedPayloadName,
                defaultEnvelopeName,
                envelopeType,
                defaultMetadataName,
                metadataType);
    }

    @SuppressWarnings("unchecked")
    public <P> Class<P> getPayloadType() {
        return (Class<P>) this.payloadType;
    }

    private static <T> Class<T> castType(final Class<?> type) {
        @SuppressWarnings("unchecked")
        final Class<T> typed = (Class<T>) type;
        return typed;
    }

    public static KafkaProducerContractBuilder<?> producerContract() {
        return KafkaProducerContractBuilder.empty();
    }

    public static KafkaConsumerContractBuilder<?> consumerContract() {
        return KafkaConsumerContractBuilder.empty();
    }
}
