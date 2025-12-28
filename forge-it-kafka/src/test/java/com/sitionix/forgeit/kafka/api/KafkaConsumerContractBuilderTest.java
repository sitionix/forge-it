package com.sitionix.forgeit.kafka.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaConsumerContractBuilderTest {

    @Test
    void shouldBuildPayloadOnlyContract() {
        final KafkaContract<UserCreatedEvent> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultExpectedPayloadName()).isEqualTo("expected.json");
        assertThat(contract.getEnvelopeType()).isNull();
        assertThat(contract.getDefaultEnvelopeName()).isNull();
        assertThat(contract.getDefaultMetadataName()).isNull();
    }

    @Test
    void shouldBuildEnvelopeThenPayloadContract() {
        final KafkaContract<UserCreatedEnvelope> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultEnvelope(UserCreatedEnvelope.class)
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getEnvelopeType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getDefaultEnvelopeName()).isNull();
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultExpectedPayloadName()).isEqualTo("expected.json");
    }

    @Test
    void shouldBuildPayloadThenEnvelopeContract() {
        final KafkaContract<UserCreatedEnvelope> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .defaultEnvelope(UserCreatedEnvelope.class)
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getEnvelopeType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getDefaultEnvelopeName()).isNull();
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultExpectedPayloadName()).isEqualTo("expected.json");
    }

    @Test
    void shouldStoreDefaultMetadataName() {
        final KafkaContract<UserCreatedEvent> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .defaultMetadata(UserCreatedMetadata.class, "metadata.json")
                .build();

        assertThat(contract.getDefaultMetadataName()).isEqualTo("metadata.json");
        assertThat(contract.getMetadataType()).isEqualTo(UserCreatedMetadata.class);
    }

    @Test
    void shouldStoreConsumerGroupId() {
        final KafkaContract<UserCreatedEvent> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .groupId("group-id")
                .build();

        assertThat(contract.getConsumerGroupId()).isEqualTo("group-id");
    }

    @Test
    void shouldRejectMissingTopic() {
        assertThatThrownBy(() -> KafkaContract.consumerContract()
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka topic must be provided");
    }

    @Test
    void shouldRejectMissingPayload() {
        assertThatThrownBy(() -> KafkaContract.consumerContract()
                .topic("topic")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("payloadType must be provided");
    }

    @Test
    void shouldRejectBlankMetadataName() {
        assertThatThrownBy(() -> KafkaContract.consumerContract()
                .topic("topic")
                .defaultMetadata(UserCreatedMetadata.class, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("metadataName must be provided");
    }

    @Test
    void shouldRejectBlankGroupId() {
        assertThatThrownBy(() -> KafkaContract.consumerContract()
                .topic("topic")
                .groupId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("groupId must be provided");
    }

    @Test
    void shouldRejectChangingMetadataType() {
        final KafkaConsumerContractBuilder<?> builder = KafkaContract.consumerContract()
                .topic("topic")
                .defaultMetadata(UserCreatedMetadata.class, "metadata.json");

        assertThatThrownBy(() -> builder.defaultMetadata(AnotherMetadata.class, "metadata.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("metadataType is already set");
    }

    @Test
    void shouldRejectChangingPayloadType() {
        final KafkaConsumerContractBuilder<?> builder = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json");

        assertThatThrownBy(() -> builder.defaultExpectedPayload(AnotherEvent.class, "expected.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("payloadType is already set");
    }

    @Test
    void shouldRejectChangingEnvelopeType() {
        final KafkaConsumerContractBuilder<?> builder = KafkaContract.consumerContract()
                .topic("topic");

        builder.defaultEnvelope(UserCreatedEnvelope.class);

        assertThatThrownBy(() -> builder.defaultEnvelope(AnotherEnvelope.class))
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
