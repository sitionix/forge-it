package com.sitionix.forgeit.kafka.internal.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.api.KafkaPublishBuilder;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public final class DefaultKafkaPublishBuilder<T> implements KafkaPublishBuilder<T> {

    private final KafkaContract<T> contract;
    private final KafkaLoader kafkaLoader;
    private final ObjectMapper objectMapper;
    private final KafkaPublisherPort publisherPort;

    private String payloadJson;
    private T payloadObject;
    private String key;

    @Override
    public KafkaPublishBuilder<T> payload(final String payloadName) {
        if (nonNull(payloadName)) {
            this.payloadObject = this.kafkaLoader.payloads().getFromFile(payloadName, this.contract.getPayloadType());
            this.payloadJson = this.writeValueAsString(this.payloadObject);
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> payload(final String payloadName, final Consumer<T> mutator) {
        if (nonNull(payloadName)) {
            this.payloadObject = this.kafkaLoader.payloads().getFromFile(payloadName, this.contract.getPayloadType());
            if (nonNull(mutator)) {
                mutator.accept(this.payloadObject);
            }
            this.payloadJson = this.writeValueAsString(this.payloadObject);
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> defaultPayload(final String payloadName) {
        if (nonNull(payloadName)) {
            this.payloadObject = this.kafkaLoader.defaultPayloads()
                    .getFromFile(payloadName, this.contract.getPayloadType());
            this.payloadJson = this.writeValueAsString(this.payloadObject);
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> defaultPayload(final String payloadName, final Consumer<T> mutator) {
        if (nonNull(payloadName)) {
            this.payloadObject = this.kafkaLoader.defaultPayloads()
                    .getFromFile(payloadName, this.contract.getPayloadType());
            if (nonNull(mutator)) {
                mutator.accept(this.payloadObject);
            }
            this.payloadJson = this.writeValueAsString(this.payloadObject);
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> defaultPayload() {
        final String payloadName = this.contract.getDefaultPayloadName();
        if (payloadName == null || payloadName.isBlank()) {
            throw new IllegalStateException("Kafka default payload is not configured");
        }
        return this.defaultPayload(payloadName);
    }

    @Override
    public KafkaPublishBuilder<T> defaultPayload(final Consumer<T> mutator) {
        final String payloadName = this.contract.getDefaultPayloadName();
        if (payloadName == null || payloadName.isBlank()) {
            throw new IllegalStateException("Kafka default payload is not configured");
        }
        return this.defaultPayload(payloadName, mutator);
    }

    @Override
    public KafkaPublishBuilder<T> payloadJson(final String payloadJson) {
        if (nonNull(payloadJson)) {
            this.payloadJson = payloadJson;
            this.payloadObject = null;
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> mutate(final Consumer<T> mutator) {
        if (mutator == null) {
            return this;
        }
        if (this.payloadObject == null) {
            this.payloadObject = this.readValue(this.payloadJson);
        }
        mutator.accept(this.payloadObject);
        this.payloadJson = this.writeValueAsString(this.payloadObject);
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> key(final String key) {
        if (nonNull(key)) {
            this.key = key;
        }
        return this;
    }

    @Override
    public void send() {
        if (this.payloadJson == null) {
            throw new IllegalStateException("Kafka payload is not configured");
        }
        this.publisherPort.publish(this.contract, this.payloadJson, this.key);
    }

    private String writeValueAsString(final Object value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka payload", ex);
        }
    }

    private T readValue(final String payloadJson) {
        if (payloadJson == null) {
            throw new IllegalStateException("Kafka payload is not configured");
        }
        try {
            return this.objectMapper.readValue(payloadJson, this.contract.getPayloadType());
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize Kafka payload", ex);
        }
    }
}
