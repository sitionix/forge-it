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
    private final String consumerGroupId;
    private final String defaultEnvelopeName;
    private final Class<?> envelopeType;
    private final String defaultMetadataName;
    private final Class<?> metadataType;

    public static <T> Builder<T> builder(final Class<T> rootType) {
        return new Builder<>(rootType);
    }

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<T> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName) {
        return builder(payloadType)
                .topic(topic)
                .defaultPayloadName(defaultPayloadName)
                .defaultExpectedPayloadName(defaultExpectedPayloadName)
                .build();
    }

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<?> rootType,
                                                      final Class<?> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName,
                                                      final String defaultEnvelopeName,
                                                      final Class<?> envelopeType) {
        return KafkaContract.<T>builder(castType(rootType))
                .topic(topic)
                .payloadType(payloadType)
                .defaultPayloadName(defaultPayloadName)
                .defaultExpectedPayloadName(defaultExpectedPayloadName)
                .defaultEnvelopeName(defaultEnvelopeName)
                .envelopeType(envelopeType)
                .build();
    }

    public static <T> KafkaContract<T> createContract(final String topic,
                                                      final Class<?> rootType,
                                                      final Class<?> payloadType,
                                                      final String defaultPayloadName,
                                                      final String defaultExpectedPayloadName,
                                                      final String consumerGroupId,
                                                      final String defaultEnvelopeName,
                                                      final Class<?> envelopeType,
                                                      final String defaultMetadataName,
                                                      final Class<?> metadataType) {
        return KafkaContract.<T>builder(castType(rootType))
                .topic(topic)
                .payloadType(payloadType)
                .defaultPayloadName(defaultPayloadName)
                .defaultExpectedPayloadName(defaultExpectedPayloadName)
                .consumerGroupId(consumerGroupId)
                .defaultEnvelopeName(defaultEnvelopeName)
                .envelopeType(envelopeType)
                .defaultMetadataName(defaultMetadataName)
                .metadataType(metadataType)
                .build();
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

    public static final class Builder<T> {

        private final Class<T> rootType;
        private String topic;
        private Class<?> payloadType;
        private String defaultPayloadName;
        private String defaultExpectedPayloadName;
        private String consumerGroupId;
        private String defaultEnvelopeName;
        private Class<?> envelopeType;
        private boolean envelopeTypeConfigured;
        private String defaultMetadataName;
        private Class<?> metadataType;

        private Builder(final Class<T> rootType) {
            this.rootType = rootType;
        }

        public Builder<T> topic(final String topic) {
            this.topic = topic;
            return this;
        }

        public Builder<T> payloadType(final Class<?> payloadType) {
            this.payloadType = payloadType;
            return this;
        }

        public Builder<T> defaultPayloadName(final String defaultPayloadName) {
            this.defaultPayloadName = defaultPayloadName;
            return this;
        }

        public Builder<T> defaultExpectedPayloadName(final String defaultExpectedPayloadName) {
            this.defaultExpectedPayloadName = defaultExpectedPayloadName;
            return this;
        }

        public Builder<T> consumerGroupId(final String consumerGroupId) {
            this.consumerGroupId = consumerGroupId;
            return this;
        }

        public Builder<T> defaultEnvelopeName(final String defaultEnvelopeName) {
            this.defaultEnvelopeName = defaultEnvelopeName;
            return this;
        }

        public Builder<T> envelopeType(final Class<?> envelopeType) {
            this.envelopeType = envelopeType;
            this.envelopeTypeConfigured = true;
            return this;
        }

        public Builder<T> defaultMetadataName(final String defaultMetadataName) {
            this.defaultMetadataName = defaultMetadataName;
            return this;
        }

        public Builder<T> metadataType(final Class<?> metadataType) {
            this.metadataType = metadataType;
            return this;
        }

        public KafkaContract<T> build() {
            final Class<?> resolvedPayloadType = this.payloadType != null ? this.payloadType : this.rootType;
            final Class<?> resolvedEnvelopeType = this.resolveEnvelopeType(resolvedPayloadType);
            return new KafkaContract<>(this.topic,
                    this.rootType,
                    resolvedPayloadType,
                    this.defaultPayloadName,
                    this.defaultExpectedPayloadName,
                    this.consumerGroupId,
                    this.defaultEnvelopeName,
                    resolvedEnvelopeType,
                    this.defaultMetadataName,
                    this.metadataType);
        }

        private Class<?> resolveEnvelopeType(final Class<?> resolvedPayloadType) {
            if (this.envelopeTypeConfigured) {
                return this.envelopeType;
            }
            if (this.rootType != null
                    && resolvedPayloadType != null
                    && !this.rootType.equals(resolvedPayloadType)) {
                return this.rootType;
            }
            return null;
        }
    }
}
