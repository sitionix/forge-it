package com.sitionix.forgeit.kafka.api;

public final class KafkaProducerContractBuilder<T> {

    private Class<?> payloadType;
    private String topic;
    private String defaultPayloadName;

    private KafkaProducerContractBuilder(final Class<?> payloadType) {
        this.payloadType = payloadType;
    }

    public static <T> KafkaProducerContractBuilder<T> forPayload(final Class<T> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        return new KafkaProducerContractBuilder<>(payloadType);
    }

    public static KafkaProducerContractBuilder<?> empty() {
        return new KafkaProducerContractBuilder<>(null);
    }

    public KafkaProducerContractBuilder<T> topic(final String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaProducerContractBuilder<T> topicFromProperty(final String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must be provided");
        }
        this.topic = "${" + propertyKey + "}";
        return this;
    }

    public KafkaProducerContractBuilder<T> defaultPayload(final String payloadName) {
        this.defaultPayloadName = payloadName;
        return this;
    }

    public <U> KafkaProducerContractBuilder<U> defaultPayload(final Class<U> payloadType, final String payloadName) {
        this.assignPayloadType(payloadType);
        this.defaultPayloadName = payloadName;
        @SuppressWarnings("unchecked")
        final KafkaProducerContractBuilder<U> typedBuilder = (KafkaProducerContractBuilder<U>) this;
        return typedBuilder;
    }

    public KafkaContract<T> build() {
        if (this.topic == null || this.topic.isBlank()) {
            throw new IllegalStateException("Kafka topic must be provided");
        }
        if (this.payloadType == null) {
            throw new IllegalStateException("payloadType must be provided");
        }
        @SuppressWarnings("unchecked")
        final Class<T> typedPayloadType = (Class<T>) this.payloadType;
        return KafkaContract.createContract(this.topic, typedPayloadType, this.defaultPayloadName, null);
    }

    private void assignPayloadType(final Class<?> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        if (this.payloadType != null && !this.payloadType.equals(payloadType)) {
            throw new IllegalStateException("payloadType is already set");
        }
        this.payloadType = payloadType;
    }
}
