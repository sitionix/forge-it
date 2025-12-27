package com.sitionix.forgeit.kafka.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaProducerContractBuilderTest {

    @Test
    void shouldBuildPayloadOnlyContract() {
        final KafkaContract<UserCreatedEvent> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultPayload(UserCreatedEvent.class, "payload.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultPayloadName()).isEqualTo("payload.json");
        assertThat(contract.getEnvelopeType()).isNull();
        assertThat(contract.getDefaultEnvelopeName()).isNull();
        assertThat(contract.getDefaultMetadataName()).isNull();
    }

    @Test
    void shouldBuildEnvelopeThenPayloadContract() {
        final KafkaContract<UserCreatedEnvelope> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultEnvelope(UserCreatedEnvelope.class, "envelope.json")
                .defaultPayload(UserCreatedEvent.class, "payload.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getEnvelopeType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getDefaultEnvelopeName()).isEqualTo("envelope.json");
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultPayloadName()).isEqualTo("payload.json");
    }

    @Test
    void shouldBuildPayloadThenEnvelopeContract() {
        final KafkaContract<UserCreatedEnvelope> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultPayload(UserCreatedEvent.class, "payload.json")
                .defaultEnvelope(UserCreatedEnvelope.class, "envelope.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getEnvelopeType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getDefaultEnvelopeName()).isEqualTo("envelope.json");
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultPayloadName()).isEqualTo("payload.json");
    }

    @Test
    void shouldStoreDefaultMetadataName() {
        final KafkaContract<UserCreatedEvent> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultPayload(UserCreatedEvent.class, "payload.json")
                .defaultMetadata(UserCreatedMetadata.class, "metadata.json")
                .build();

        assertThat(contract.getDefaultMetadataName()).isEqualTo("metadata.json");
        assertThat(contract.getMetadataType()).isEqualTo(UserCreatedMetadata.class);
    }

    @Test
    void shouldRejectMissingTopic() {
        assertThatThrownBy(() -> KafkaContract.producerContract()
                .defaultPayload(UserCreatedEvent.class, "payload.json")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka topic must be provided");
    }

    @Test
    void shouldRejectMissingPayload() {
        assertThatThrownBy(() -> KafkaContract.producerContract()
                .topic("topic")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("payloadType must be provided");
    }

    @Test
    void shouldRejectBlankEnvelopeName() {
        assertThatThrownBy(() -> KafkaContract.producerContract()
                .topic("topic")
                .defaultEnvelope(UserCreatedEnvelope.class, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("envelopeName must be provided");
    }

    @Test
    void shouldRejectBlankMetadataName() {
        assertThatThrownBy(() -> KafkaContract.producerContract()
                .topic("topic")
                .defaultMetadata(UserCreatedMetadata.class, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("metadataName must be provided");
    }

    @Test
    void shouldRejectChangingMetadataType() {
        final KafkaProducerContractBuilder<?> builder = KafkaContract.producerContract()
                .topic("topic")
                .defaultMetadata(UserCreatedMetadata.class, "metadata.json");

        assertThatThrownBy(() -> builder.defaultMetadata(AnotherMetadata.class, "metadata.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("metadataType is already set");
    }

    @Test
    void shouldRejectChangingPayloadType() {
        final KafkaProducerContractBuilder<?> builder = KafkaContract.producerContract()
                .topic("topic")
                .defaultPayload(UserCreatedEvent.class, "payload.json");

        assertThatThrownBy(() -> builder.defaultPayload(AnotherEvent.class, "payload.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("payloadType is already set");
    }

    @Test
    void shouldRejectChangingEnvelopeType() {
        final KafkaProducerContractBuilder<?> builder = KafkaContract.producerContract()
                .topic("topic");

        builder.defaultEnvelope(UserCreatedEnvelope.class, "envelope.json");

        assertThatThrownBy(() -> builder.defaultEnvelope(AnotherEnvelope.class, "envelope-v2.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("envelopeType is already set");
    }

    private static final class UserCreatedEvent {
    }

    private static final class AnotherEvent {
    }

    private static final class UserCreatedEnvelope {
    }

    private static final class AnotherEnvelope {
    }

    private static final class UserCreatedMetadata {
    }

    private static final class AnotherMetadata {
    }
}
