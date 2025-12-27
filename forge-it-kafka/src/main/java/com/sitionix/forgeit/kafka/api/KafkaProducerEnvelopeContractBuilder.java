package com.sitionix.forgeit.kafka.api;

public interface KafkaProducerEnvelopeContractBuilder<T> {

    KafkaProducerEnvelopeContractBuilder<T> topic(String topic);

    KafkaProducerEnvelopeContractBuilder<T> topicFromProperty(String propertyKey);

    <U> KafkaProducerEnvelopeContractBuilder<T> defaultPayload(Class<U> payloadType, String payloadName);

    <M> KafkaProducerEnvelopeContractBuilder<T> defaultMetadata(Class<M> metadataType, String metadataName);

    KafkaProducerEnvelopeContractBuilder<T> defaultEnvelope(Class<T> envelopeType);

    KafkaContract<T> build();
}
