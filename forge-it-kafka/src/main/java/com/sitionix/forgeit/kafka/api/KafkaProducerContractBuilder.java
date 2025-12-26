package com.sitionix.forgeit.kafka.api;

public final class KafkaProducerContractBuilder<T> {

    private final Class<T> payloadType;
    private String topic;
    private String defaultPayloadName;

    private KafkaProducerContractBuilder(final Class<T> payloadType) {
        this.payloadType = payloadType;
    }

    public static <T> KafkaProducerContractBuilder<T> forPayload(final Class<T> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        return new KafkaProducerContractBuilder<>(payloadType);
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

    public KafkaContract<T> build() {
        if (this.topic == null || this.topic.isBlank()) {
            throw new IllegalStateException("Kafka topic must be provided");
        }
        return KafkaContract.createContract(this.topic, this.payloadType, this.defaultPayloadName, null);
    }
}
