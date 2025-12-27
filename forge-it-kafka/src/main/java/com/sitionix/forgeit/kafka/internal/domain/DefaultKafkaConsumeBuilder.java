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
    private String consumedPayloadJson;

    @Override
    public KafkaConsumeBuilder<T> await(final Duration timeout) {
        if (timeout != null) {
            this.timeout = timeout;
        }
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertPayload() {
        final String payloadName = this.resolveDefaultExpectedPayloadName();
        this.assertPayloadInternal(payloadName, null, true);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertPayload(final Consumer<T> mutator) {
        final String payloadName = this.resolveDefaultExpectedPayloadName();
        this.assertPayloadInternal(payloadName, mutator, true);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertPayload(final String payloadName) {
        if (!nonNull(payloadName)) {
            return this;
        }
        this.assertPayloadInternal(payloadName, null, false);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertPayload(final String payloadName, final Consumer<T> mutator) {
        if (!nonNull(payloadName)) {
            return this;
        }
        this.assertPayloadInternal(payloadName, mutator, false);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertMetadata(final Consumer<T> mutator) {
        this.assertMetadataInternal(this.resolveDefaultMetadataName(), mutator, true);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertMetadata(final String metadataName) {
        if (!nonNull(metadataName)) {
            return this;
        }
        this.assertMetadataInternal(metadataName, null, false);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertMetadata(final String metadataName, final Consumer<T> mutator) {
        if (!nonNull(metadataName)) {
            return this;
        }
        this.assertMetadataInternal(metadataName, mutator, false);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertEnvelope(final Consumer<T> mutator) {
        if (mutator == null) {
            return this;
        }
        this.ensureEnvelopeConfigured("envelope");
        final Object actualEnvelope = this.readRoot(this.consume());
        @SuppressWarnings("unchecked")
        final T typedEnvelope = (T) actualEnvelope;
        mutator.accept(typedEnvelope);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertEnvelope(final String envelopeName) {
        if (!nonNull(envelopeName)) {
            return this;
        }
        this.assertEnvelopeInternal(envelopeName, null);
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertEnvelope(final String envelopeName, final Consumer<T> mutator) {
        if (!nonNull(envelopeName)) {
            return this;
        }
        this.assertEnvelopeInternal(envelopeName, mutator);
        return this;
    }

    private void assertPayloadInternal(final String payloadName,
                                       final Consumer<T> mutator,
                                       final boolean useDefaultPayloadPath) {
        final Object expectedPayload = this.loadExpectedPayload(payloadName, useDefaultPayloadPath);
        if (this.contract.getEnvelopeType() == null) {
            this.applyMutatorToPayload(expectedPayload, mutator);
            this.compareObjects(expectedPayload, this.readRoot(this.consume()));
            return;
        }
        final Object expectedEnvelope = this.createExpectedEnvelope(expectedPayload);
        this.applyMutatorToEnvelope(expectedEnvelope, mutator);
        final Object expectedPayloadValue = KafkaEnvelopeBinding.extractPayload(expectedEnvelope,
                this.contract.getPayloadType());
        final Object actualEnvelope = this.readRoot(this.consume());
        final Object actualPayload = KafkaEnvelopeBinding.extractPayload(actualEnvelope,
                this.contract.getPayloadType());
        this.compareObjects(expectedPayloadValue, actualPayload);
    }

    private void assertMetadataInternal(final String metadataName,
                                        final Consumer<T> mutator,
                                        final boolean useDefaultMetadataPath) {
        this.ensureEnvelopeConfigured("metadata");
        final Object expectedMetadata = this.loadExpectedMetadata(metadataName, useDefaultMetadataPath);
        final Object expectedEnvelope = this.createEnvelopeForMetadata(expectedMetadata);
        this.applyMutatorToEnvelope(expectedEnvelope, mutator);
        final Object expectedMetadataValue = KafkaEnvelopeBinding.extractMetadata(expectedEnvelope,
                this.contract.getMetadataType());
        final Object actualEnvelope = this.readRoot(this.consume());
        final Object actualMetadata = KafkaEnvelopeBinding.extractMetadata(actualEnvelope,
                this.contract.getMetadataType());
        this.compareObjects(expectedMetadataValue, actualMetadata);
    }

    private void assertEnvelopeInternal(final String envelopeName, final Consumer<T> mutator) {
        this.ensureEnvelopeConfigured("envelope");
        final Object expectedEnvelope = this.loadExpectedEnvelope(envelopeName);
        this.applyMutatorToEnvelope(expectedEnvelope, mutator);
        final Object actualEnvelope = this.readRoot(this.consume());
        this.compareObjects(expectedEnvelope, actualEnvelope);
    }

    private Object createExpectedEnvelope(final Object payload) {
        final Object envelope = KafkaEnvelopeBinding.createEnvelope(this.contract.getEnvelopeType());
        KafkaEnvelopeBinding.injectPayload(envelope, payload, this.contract.getPayloadType());
        final Object metadata = this.resolveDefaultMetadataIfConfigured();
        KafkaEnvelopeBinding.injectMetadata(envelope, metadata, this.contract.getMetadataType());
        return envelope;
    }

    private Object createEnvelopeForMetadata(final Object metadata) {
        final Object envelope = KafkaEnvelopeBinding.createEnvelope(this.contract.getEnvelopeType());
        KafkaEnvelopeBinding.injectPayload(envelope, this.resolveDefaultPayloadIfConfigured(),
                this.contract.getPayloadType());
        KafkaEnvelopeBinding.injectMetadata(envelope, metadata, this.contract.getMetadataType());
        return envelope;
    }

    private Object loadExpectedPayload(final String payloadName, final boolean useDefaultPath) {
        if (useDefaultPath) {
            return this.kafkaLoader.defaultExpectedPayloads()
                    .getFromFile(payloadName, this.contract.getPayloadType());
        }
        return this.kafkaLoader.expectedPayloads()
                .getFromFile(payloadName, this.contract.getPayloadType());
    }

    private Object loadExpectedMetadata(final String metadataName, final boolean useDefaultPath) {
        if (useDefaultPath) {
            return this.kafkaLoader.defaultMetadata()
                    .getFromFile(metadataName, this.contract.getMetadataType());
        }
        return this.kafkaLoader.metadata()
                .getFromFile(metadataName, this.contract.getMetadataType());
    }

    private Object loadExpectedEnvelope(final String envelopeName) {
        return this.kafkaLoader.expectedPayloads()
                .getFromFile(envelopeName, this.contract.getEnvelopeType());
    }

    private Object resolveDefaultPayloadIfConfigured() {
        final String payloadName = this.contract.getDefaultExpectedPayloadName();
        if (payloadName == null || payloadName.isBlank()) {
            return null;
        }
        return this.kafkaLoader.defaultExpectedPayloads()
                .getFromFile(payloadName, this.contract.getPayloadType());
    }

    private Object resolveDefaultMetadataIfConfigured() {
        if (this.contract.getMetadataType() == null) {
            return null;
        }
        final String metadataName = this.contract.getDefaultMetadataName();
        if (metadataName == null || metadataName.isBlank()) {
            return null;
        }
        return this.kafkaLoader.defaultMetadata()
                .getFromFile(metadataName, this.contract.getMetadataType());
    }

    private void applyMutatorToEnvelope(final Object envelope, final Consumer<T> mutator) {
        if (mutator == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final T typedEnvelope = (T) envelope;
        mutator.accept(typedEnvelope);
    }

    private void applyMutatorToPayload(final Object payload, final Consumer<T> mutator) {
        if (mutator == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final T typedPayload = (T) payload;
        mutator.accept(typedPayload);
    }

    private String consume() {
        if (this.consumedPayloadJson == null) {
            this.consumedPayloadJson = this.consumerPort.consume(this.contract, this.timeout);
        }
        return this.consumedPayloadJson;
    }

    private Object readRoot(final String payloadJson) {
        try {
            return this.objectMapper.readValue(payloadJson, this.contract.getRootType());
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize Kafka payload", ex);
        }
    }

    private void compareObjects(final Object expected, final Object actual) {
        try {
            final JsonNode expectedNode = this.objectMapper.valueToTree(expected);
            final JsonNode actualNode = this.objectMapper.valueToTree(actual);
            if (!expectedNode.equals(actualNode)) {
                throw new IllegalStateException("Kafka payload does not match expected fixture");
            }
        } catch (final IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to compare Kafka payload JSON", ex);
        }
    }

    private String resolveDefaultExpectedPayloadName() {
        final String payloadName = this.contract.getDefaultExpectedPayloadName();
        if (payloadName == null || payloadName.isBlank()) {
            throw new IllegalStateException("Kafka default expected payload is not configured");
        }
        return payloadName;
    }

    private String resolveDefaultMetadataName() {
        final String metadataName = this.contract.getDefaultMetadataName();
        if (metadataName == null || metadataName.isBlank()) {
            throw new IllegalStateException("Kafka default metadata is not configured");
        }
        return metadataName;
    }

    private void ensureEnvelopeConfigured(final String label) {
        if (this.contract.getEnvelopeType() == null) {
            throw new IllegalStateException("Kafka " + label + " requires an envelope type");
        }
        if ("metadata".equals(label) && this.contract.getMetadataType() == null) {
            throw new IllegalStateException("Kafka metadata type is not configured");
        }
    }

}
