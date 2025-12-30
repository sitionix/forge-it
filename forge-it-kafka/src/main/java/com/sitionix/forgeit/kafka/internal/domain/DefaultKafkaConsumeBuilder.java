package com.sitionix.forgeit.kafka.internal.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sitionix.forgeit.kafka.api.KafkaConsumeBuilder;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public final class DefaultKafkaConsumeBuilder<T> implements KafkaConsumeBuilder<T> {

    private final KafkaContract<T> contract;
    private final KafkaLoader kafkaLoader;
    private final ObjectMapper objectMapper;
    private final KafkaConsumerPort consumerPort;

    private Duration timeout;
    private Object consumedPayload;
    private final Set<String> ignoredFields = new LinkedHashSet<>();

    @Override
    public KafkaConsumeBuilder<T> await(final Duration timeout) {
        if (timeout != null) {
            this.timeout = timeout;
        }
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertAny() {
        if (this.consume() == null) {
            throw new IllegalStateException("Kafka payload was not consumed");
        }
        return this;
    }

    @Override
    public KafkaConsumeBuilder<T> assertNone() {
        if (this.consumedPayload != null) {
            throw new IllegalStateException("Kafka payload was already consumed");
        }
        if (this.consumeIfPresent() != null) {
            throw new IllegalStateException("Kafka message was received but none was expected");
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

    @Override
    public KafkaConsumeBuilder<T> ignoreFields(final String... fields) {
        if (fields == null) {
            return this;
        }
        for (final String field : fields) {
            if (field != null && !field.isBlank()) {
                this.ignoredFields.add(field);
            }
        }
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

    private Object consume() {
        if (this.consumedPayload == null) {
            this.consumedPayload = this.consumerPort.consume(this.contract, this.timeout);
        }
        return this.consumedPayload;
    }

    private Object consumeIfPresent() {
        return this.consumerPort.consumeIfPresent(this.contract, this.timeout);
    }

    private Object readRoot(final Object payload) {
        if (this.contract.getPayloadDeserializerClass() != null) {
            return this.deserializeWithKafkaDeserializer(payload, this.contract.getRootType());
        }
        return this.deserializeWithObjectMapper(payload, this.contract.getRootType());
    }

    @SuppressWarnings("rawtypes")
    private Object deserializeWithKafkaDeserializer(final Object payload, final Class<?> targetType) {
        if (payload == null) {
            throw new IllegalStateException("Kafka payload is not configured");
        }
        if (targetType.isInstance(payload)) {
            return payload;
        }
        if (!(payload instanceof byte[] payloadBytes)) {
            throw new IllegalStateException("Kafka payload deserializer expects byte[] but received "
                    + payload.getClass().getName());
        }
        final org.apache.kafka.common.serialization.Deserializer deserializer =
                KafkaSerdeSupport.createDeserializer(this.contract.getPayloadDeserializerClass(), targetType);
        final Object decoded = deserializer.deserialize(this.contract.getTopic(), payloadBytes);
        if (decoded == null) {
            throw new IllegalStateException("Kafka payload deserializer returned null");
        }
        if (!targetType.isInstance(decoded)) {
            throw new IllegalStateException("Kafka payload deserializer returned unsupported type "
                    + decoded.getClass().getName());
        }
        return decoded;
    }

    private Object deserializeWithObjectMapper(final Object payload, final Class<?> targetType) {
        if (targetType.isInstance(payload)) {
            return payload;
        }
        try {
            if (payload instanceof String payloadJson) {
                return this.objectMapper.readValue(payloadJson, targetType);
            }
            if (payload instanceof byte[] payloadBytes) {
                return this.objectMapper.readValue(payloadBytes, targetType);
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to deserialize Kafka payload", ex);
        }
        if (payload == null) {
            throw new IllegalStateException("Kafka payload is not configured");
        }
        throw new IllegalStateException("Kafka payload value is not supported for deserialization: "
                + payload.getClass().getName());
    }

    private void compareObjects(final Object expected, final Object actual) {
        try {
            JsonNode expectedNode = this.toJsonNode(expected);
            JsonNode actualNode = this.toJsonNode(actual);
            if (!this.ignoredFields.isEmpty()) {
                expectedNode = this.removeIgnoredFields(expectedNode.deepCopy());
                actualNode = this.removeIgnoredFields(actualNode.deepCopy());
            }
            if (!expectedNode.equals(actualNode)) {
                throw new IllegalStateException("Kafka payload does not match expected fixture");
            }
        } catch (final IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to compare Kafka payload JSON", ex);
        }
    }

    private JsonNode toJsonNode(final Object value) throws IOException {
        if (value == null) {
            return this.objectMapper.nullNode();
        }
        if (this.isAvroSpecificRecord(value)) {
            return this.objectMapper.readTree(value.toString());
        }
        return this.objectMapper.valueToTree(value);
    }

    private boolean isAvroSpecificRecord(final Object value) {
        try {
            final Class<?> specificRecord = Class.forName("org.apache.avro.specific.SpecificRecord");
            return specificRecord.isInstance(value);
        } catch (final ClassNotFoundException ex) {
            return false;
        }
    }

    private JsonNode removeIgnoredFields(final JsonNode node) {
        if (node == null || this.ignoredFields.isEmpty()) {
            return node;
        }
        if (node.isObject()) {
            final ObjectNode objectNode = (ObjectNode) node;
            final List<String> fieldsToRemove = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (this.ignoredFields.contains(fieldName)) {
                    fieldsToRemove.add(fieldName);
                } else {
                    this.removeIgnoredFields(objectNode.get(fieldName));
                }
            });
            objectNode.remove(fieldsToRemove);
            return objectNode;
        }
        if (node.isArray()) {
            final ArrayNode arrayNode = (ArrayNode) node;
            for (final JsonNode item : arrayNode) {
                this.removeIgnoredFields(item);
            }
            return arrayNode;
        }
        return node;
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
