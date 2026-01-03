package com.sitionix.forgeit.kafka.internal.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.api.KafkaPublishBuilder;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public final class DefaultKafkaPublishBuilder<T> implements KafkaPublishBuilder<T> {

    private final KafkaContract<T> contract;
    private final KafkaLoader kafkaLoader;
    private final ObjectMapper objectMapper;
    private final KafkaPublisherPort publisherPort;

    private String payloadJson;
    private Object payloadObject;
    private Object metadataObject;
    private String key;
    private final List<Consumer<T>> payloadMutators = new ArrayList<>();
    private final List<Consumer<T>> metadataMutators = new ArrayList<>();
    private final List<Consumer<T>> envelopeMutators = new ArrayList<>();

    @Override
    public KafkaPublishBuilder<T> payload(final Consumer<T> mutator) {
        if (mutator == null) {
            return this;
        }
        this.resolveDefaultPayload();
        if (this.contract.getEnvelopeType() != null) {
            this.payloadMutators.add(mutator);
            return this;
        }
        this.applyPayloadMutator(mutator);
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> payload(final String payloadName) {
        if (nonNull(payloadName)) {
            this.payloadObject = this.kafkaLoader.payloads().getFromFile(payloadName, this.contract.getPayloadType());
            this.payloadJson = null;
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> payload(final String payloadName, final Consumer<T> mutator) {
        if (nonNull(payloadName)) {
            this.payloadObject = this.kafkaLoader.payloads().getFromFile(payloadName, this.contract.getPayloadType());
            this.payloadJson = null;
            if (nonNull(mutator)) {
                if (this.contract.getEnvelopeType() != null) {
                    this.payloadMutators.add(mutator);
                } else {
                    this.applyPayloadMutator(mutator);
                }
            }
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> metadata(final Consumer<T> mutator) {
        if (mutator == null) {
            return this;
        }
        this.ensureEnvelopeConfigured("metadata");
        this.resolveDefaultMetadata();
        this.metadataMutators.add(mutator);
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> metadata(final String metadataName) {
        if (nonNull(metadataName)) {
            this.ensureEnvelopeConfigured("metadata");
            this.metadataObject = this.kafkaLoader.metadata()
                    .getFromFile(metadataName, this.contract.getMetadataType());
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> metadata(final String metadataName, final Consumer<T> mutator) {
        if (nonNull(metadataName)) {
            this.ensureEnvelopeConfigured("metadata");
            this.metadataObject = this.kafkaLoader.metadata()
                    .getFromFile(metadataName, this.contract.getMetadataType());
            if (nonNull(mutator)) {
                this.metadataMutators.add(mutator);
            }
        }
        return this;
    }

    @Override
    public KafkaPublishBuilder<T> envelope(final Consumer<T> mutator) {
        if (mutator == null) {
            return this;
        }
        this.ensureEnvelopeConfigured("envelope");
        this.envelopeMutators.add(mutator);
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
        this.publishMessage();
    }

    @Override
    public void sendAndVerify(final Consumer<T> verifier) {
        this.sendAndVerify(null, verifier);
    }

    @Override
    public void sendAndVerify(final Duration timeout, final Consumer<T> verifier) {
        final PublishOutcome<T> outcome = this.publishMessage();
        if (verifier == null) {
            return;
        }
        if (timeout != null) {
            Awaitility.await()
                    .atMost(timeout)
                    .untilAsserted(() -> verifier.accept(outcome.root));
            return;
        }
        Awaitility.await()
                .untilAsserted(() -> verifier.accept(outcome.root));
    }

    private PublishOutcome<T> publishMessage() {
        final PublishOutcome<T> outcome = this.createPublishOutcome();
        this.publisherPort.publish(this.contract, outcome.payloadValue, this.key);
        return outcome;
    }

    private PublishOutcome<T> createPublishOutcome() {
        final Object payload = this.resolvePayloadObject();
        if (payload == null) {
            throw new IllegalStateException("Kafka payload is not configured");
        }
        if (this.contract.getEnvelopeType() == null) {
            @SuppressWarnings("unchecked")
            final T rootPayload = (T) payload;
            return new PublishOutcome<>(rootPayload, this.serializePayload(payload));
        }
        final Object envelope = KafkaEnvelopeBinding.createEnvelope(this.contract.getEnvelopeType());
        KafkaEnvelopeBinding.injectPayload(envelope, payload, this.contract.getPayloadType());
        final Object metadata = this.resolveMetadataObject();
        KafkaEnvelopeBinding.injectMetadata(envelope, metadata, this.contract.getMetadataType());
        this.applyMutators(this.payloadMutators, envelope);
        this.applyMutators(this.metadataMutators, envelope);
        this.applyMutators(this.envelopeMutators, envelope);
        @SuppressWarnings("unchecked")
        final T rootEnvelope = (T) envelope;
        return new PublishOutcome<>(rootEnvelope, this.serializePayload(envelope));
    }

    private Object serializePayload(final Object value) {
        if (this.contract.getPayloadSerializerClass() != null) {
            return this.serializeWithKafkaSerializer(value);
        }
        return this.writeValueAsString(value);
    }

    @SuppressWarnings("rawtypes")
    private Object serializeWithKafkaSerializer(final Object value) {
        final org.apache.kafka.common.serialization.Serializer serializer =
                KafkaSerdeSupport.createSerializer(this.contract.getPayloadSerializerClass(), value.getClass());
        final Object encoded = serializer.serialize(this.contract.getTopic(), value);
        if (encoded != null && !(encoded instanceof byte[])) {
            throw new IllegalStateException("Kafka payload serializer must return byte[]");
        }
        return encoded;
    }

    private void applyMutators(final List<Consumer<T>> mutators, final Object target) {
        if (mutators.isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        final T typed = (T) target;
        for (final Consumer<T> mutator : mutators) {
            if (mutator != null) {
                mutator.accept(typed);
            }
        }
    }

    private void applyPayloadMutator(final Consumer<T> mutator) {
        if (mutator == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final T typedPayload = (T) this.resolvePayloadObject();
        mutator.accept(typedPayload);
    }

    private Object resolvePayloadObject() {
        if (this.payloadObject != null) {
            return this.payloadObject;
        }
        if (this.payloadJson != null) {
            this.payloadObject = this.readValue(this.payloadJson);
            return this.payloadObject;
        }
        final String defaultPayloadName = this.contract.getDefaultPayloadName();
        if (defaultPayloadName == null || defaultPayloadName.isBlank()) {
            return null;
        }
        this.payloadObject = this.kafkaLoader.defaultPayloads()
                .getFromFile(defaultPayloadName, this.contract.getPayloadType());
        return this.payloadObject;
    }

    private void resolveDefaultPayload() {
        if (this.payloadObject != null || this.payloadJson != null) {
            return;
        }
        this.payloadObject = this.kafkaLoader.defaultPayloads()
                .getFromFile(this.resolveDefaultPayloadName(), this.contract.getPayloadType());
    }

    private String resolveDefaultPayloadName() {
        final String payloadName = this.contract.getDefaultPayloadName();
        if (payloadName == null || payloadName.isBlank()) {
            throw new IllegalStateException("Kafka default payload is not configured");
        }
        return payloadName;
    }

    private Object resolveMetadataObject() {
        if (this.contract.getMetadataType() == null) {
            return null;
        }
        if (this.metadataObject != null) {
            return this.metadataObject;
        }
        final String defaultMetadataName = this.contract.getDefaultMetadataName();
        if (defaultMetadataName == null || defaultMetadataName.isBlank()) {
            throw new IllegalStateException("Kafka default metadata is not configured");
        }
        this.metadataObject = this.kafkaLoader.defaultMetadata()
                .getFromFile(defaultMetadataName, this.contract.getMetadataType());
        return this.metadataObject;
    }

    private void resolveDefaultMetadata() {
        if (this.metadataObject != null) {
            return;
        }
        this.metadataObject = this.kafkaLoader.defaultMetadata()
                .getFromFile(this.resolveDefaultMetadataName(), this.contract.getMetadataType());
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
        if (this.contract.getMetadataType() == null && "metadata".equals(label)) {
            throw new IllegalStateException("Kafka metadata type is not configured");
        }
    }

    private String writeValueAsString(final Object value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka payload", ex);
        }
    }

    private Object readValue(final String payloadJson) {
        if (payloadJson == null) {
            throw new IllegalStateException("Kafka payload is not configured");
        }
        try {
            return this.objectMapper.readValue(payloadJson, this.contract.getPayloadType());
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize Kafka payload", ex);
        }
    }

    private static final class PublishOutcome<T> {
        private final T root;
        private final Object payloadValue;

        private PublishOutcome(final T root, final Object payloadValue) {
            this.root = root;
            this.payloadValue = payloadValue;
        }
    }
}
