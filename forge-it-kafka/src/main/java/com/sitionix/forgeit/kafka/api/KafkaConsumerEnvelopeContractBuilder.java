package com.sitionix.forgeit.kafka.api;

public interface KafkaConsumerEnvelopeContractBuilder<T> {

    KafkaConsumerEnvelopeContractBuilder<T> topic(String topic);

    KafkaConsumerEnvelopeContractBuilder<T> topicFromProperty(String propertyKey);

    KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(String payloadName);

    <U> KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(Class<U> payloadType, String payloadName);

    <M> KafkaConsumerEnvelopeContractBuilder<T> defaultMetadata(Class<M> metadataType, String metadataName);

    KafkaConsumerEnvelopeContractBuilder<T> defaultEnvelope(Class<T> envelopeType);

    KafkaContract<T> build();
}
