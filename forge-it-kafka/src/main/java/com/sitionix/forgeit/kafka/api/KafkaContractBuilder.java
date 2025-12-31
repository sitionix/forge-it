package com.sitionix.forgeit.kafka.api;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public final class KafkaContractBuilder<T> {

    private final Class<T> payloadType;
    private String topic;
    private Class<? extends Serializer> payloadSerializerClass;
    private Class<? extends Deserializer> payloadDeserializerClass;

    private KafkaContractBuilder(final Class<T> payloadType) {
        this.payloadType = payloadType;
    }

    public static <T> KafkaContractBuilder<T> forPayload(final Class<T> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        return new KafkaContractBuilder<>(payloadType);
    }

    public KafkaContractBuilder<T> topic(final String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaContractBuilder<T> topicFromProperty(final String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must be provided");
        }
        this.topic = "${" + propertyKey + "}";
        return this;
    }

    public KafkaContractBuilder<T> payloadSerializer(
            final Class<? extends Serializer> payloadSerializerClass) {
        this.payloadSerializerClass = payloadSerializerClass;
        return this;
    }

    public KafkaContractBuilder<T> payloadDeserializer(
            final Class<? extends Deserializer> payloadDeserializerClass) {
        this.payloadDeserializerClass = payloadDeserializerClass;
        return this;
    }

    public KafkaContract<T> build() {
        if (this.topic == null || this.topic.isBlank()) {
            throw new IllegalStateException("Kafka topic must be provided");
        }
        return KafkaContract.builder(this.payloadType)
                .topic(this.topic)
                .payloadSerializer(this.payloadSerializerClass)
                .payloadDeserializer(this.payloadDeserializerClass)
                .build();
    }
}
