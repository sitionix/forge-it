package com.sitionix.forgeit.kafka.internal.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.kafka.api.KafkaConsumeBuilder;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public final class DefaultKafkaConsumeBuilder<T> implements KafkaConsumeBuilder<T> {

    private final KafkaContract<T> contract;
    private final KafkaLoader kafkaLoader;
    private final ObjectMapper objectMapper;
    private final KafkaConsumerPort consumerPort;

    private Duration timeout;

    @Override
    public KafkaConsumeBuilder<T> await(final Duration timeout) {
        if (timeout != null) {
            this.timeout = timeout;
        }
        return this;
    }

    @Override
    public void expectPayload(final String payloadName) {
        if (!nonNull(payloadName)) {
            return;
        }
        final T payloadObject = this.kafkaLoader.expectedPayloads()
                .getFromFile(payloadName, this.contract.getPayloadType());
        final String expectedJson = this.writeValueAsString(payloadObject);
        this.consumeAndAssert(expectedJson);
    }

    @Override
    public void expectPayload(final String payloadName, final Consumer<T> mutator) {
        if (!nonNull(payloadName)) {
            return;
        }
        final T payloadObject = this.kafkaLoader.expectedPayloads()
                .getFromFile(payloadName, this.contract.getPayloadType());
        if (nonNull(mutator)) {
            mutator.accept(payloadObject);
        }
        final String expectedJson = this.writeValueAsString(payloadObject);
        this.consumeAndAssert(expectedJson);
    }

    @Override
    public void defaultExpectPayload(final String payloadName) {
        if (!nonNull(payloadName)) {
            return;
        }
        final T payloadObject = this.kafkaLoader.defaultExpectedPayloads()
                .getFromFile(payloadName, this.contract.getPayloadType());
        final String expectedJson = this.writeValueAsString(payloadObject);
        this.consumeAndAssert(expectedJson);
    }

    @Override
    public void defaultExpectPayload(final String payloadName, final Consumer<T> mutator) {
        if (!nonNull(payloadName)) {
            return;
        }
        final T payloadObject = this.kafkaLoader.defaultExpectedPayloads()
                .getFromFile(payloadName, this.contract.getPayloadType());
        if (nonNull(mutator)) {
            mutator.accept(payloadObject);
        }
        final String expectedJson = this.writeValueAsString(payloadObject);
        this.consumeAndAssert(expectedJson);
    }

    private void consumeAndAssert(final String expectedJson) {
        final String payloadJson = this.consumerPort.consume(this.contract, this.timeout);
        try {
            final JsonNode expected = this.objectMapper.readTree(expectedJson);
            final JsonNode actual = this.objectMapper.readTree(payloadJson);
            if (!expected.equals(actual)) {
                throw new IllegalStateException("Kafka payload does not match expected fixture");
            }
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to compare Kafka payload JSON", ex);
        }
    }

    private String writeValueAsString(final Object value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka payload", ex);
        }
    }

}
