package com.sitionix.forgeit.kafka.api;

public final class KafkaConsumerContractBuilder<T> {

    private Class<?> payloadType;
    private String topic;
    private String defaultExpectedPayloadName;

    private KafkaConsumerContractBuilder(final Class<?> payloadType) {
        this.payloadType = payloadType;
    }

    public static <T> KafkaConsumerContractBuilder<T> forPayload(final Class<T> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        return new KafkaConsumerContractBuilder<>(payloadType);
    }

    public static KafkaConsumerContractBuilder<?> empty() {
        return new KafkaConsumerContractBuilder<>(null);
    }

    public KafkaConsumerContractBuilder<T> topic(final String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaConsumerContractBuilder<T> topicFromProperty(final String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must be provided");
        }
        this.topic = "${" + propertyKey + "}";
        return this;
    }

    public KafkaConsumerContractBuilder<T> defaultExpectedPayload(final String payloadName) {
        this.defaultExpectedPayloadName = payloadName;
        return this;
    }

    public <U> KafkaConsumerContractBuilder<U> defaultExpectedPayload(final Class<U> payloadType,
                                                                      final String payloadName) {
        this.assignPayloadType(payloadType);
        this.defaultExpectedPayloadName = payloadName;
        @SuppressWarnings("unchecked")
        final KafkaConsumerContractBuilder<U> typedBuilder = (KafkaConsumerContractBuilder<U>) this;
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
        return KafkaContract.createContract(this.topic, typedPayloadType, null, this.defaultExpectedPayloadName);
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
