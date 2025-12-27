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
    }

    @Test
    void shouldBuildEnvelopeThenPayloadContract() {
        final KafkaContract<UserCreatedEnvelope> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultEnvelope(UserCreatedEnvelope.class, "envelope.json")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getEnvelopeType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getDefaultEnvelopeName()).isEqualTo("envelope.json");
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultExpectedPayloadName()).isEqualTo("expected.json");
    }

    @Test
    void shouldBuildPayloadThenEnvelopeContract() {
        final KafkaContract<UserCreatedEnvelope> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(UserCreatedEvent.class, "expected.json")
                .defaultEnvelope(UserCreatedEnvelope.class, "envelope.json")
                .build();

        assertThat(contract.getRootType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getEnvelopeType()).isEqualTo(UserCreatedEnvelope.class);
        assertThat(contract.getDefaultEnvelopeName()).isEqualTo("envelope.json");
        assertThat(contract.getPayloadType()).isEqualTo(UserCreatedEvent.class);
        assertThat(contract.getDefaultExpectedPayloadName()).isEqualTo("expected.json");
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
    void shouldRejectBlankEnvelopeName() {
        assertThatThrownBy(() -> KafkaContract.consumerContract()
                .topic("topic")
                .defaultEnvelope(UserCreatedEnvelope.class, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("envelopeName must be provided");
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
}
