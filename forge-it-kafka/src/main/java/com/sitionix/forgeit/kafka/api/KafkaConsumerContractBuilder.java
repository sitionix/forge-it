package com.sitionix.forgeit.kafka.api;

public final class KafkaConsumerContractBuilder<T> {

    private final Class<T> payloadType;
    private String topic;
    private String defaultExpectedPayloadName;

    private KafkaConsumerContractBuilder(final Class<T> payloadType) {
        this.payloadType = payloadType;
    }

    public static <T> KafkaConsumerContractBuilder<T> forPayload(final Class<T> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        return new KafkaConsumerContractBuilder<>(payloadType);
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

    public KafkaContract<T> build() {
        if (this.topic == null || this.topic.isBlank()) {
            throw new IllegalStateException("Kafka topic must be provided");
        }
        return KafkaContract.createContract(this.topic, this.payloadType, null, this.defaultExpectedPayloadName);
    }
}
