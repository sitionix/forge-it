package com.sitionix.forgeit.kafka.api;

public interface KafkaConsumerEnvelopeContractBuilder<T> {

    KafkaConsumerEnvelopeContractBuilder<T> topic(String topic);

    KafkaConsumerEnvelopeContractBuilder<T> topicFromProperty(String propertyKey);

    KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(String payloadName);

    <U> KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(Class<U> payloadType, String payloadName);

    KafkaConsumerEnvelopeContractBuilder<T> defaultEnvelope(Class<T> envelopeType, String envelopeName);

    KafkaContract<T> build();
}
