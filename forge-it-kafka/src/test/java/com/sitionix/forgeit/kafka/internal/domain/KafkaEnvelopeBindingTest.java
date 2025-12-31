package com.sitionix.forgeit.kafka.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaEnvelopeBindingTest {

    @Test
    void shouldInjectAndExtractPayloadAndMetadataUsingFields() {
        final EnvelopeWithFields envelope = KafkaEnvelopeBinding.createEnvelope(EnvelopeWithFields.class);
        final Payload payload = new Payload("user-1");
        final Metadata metadata = new Metadata("trace-1");

        KafkaEnvelopeBinding.injectPayload(envelope, payload, Payload.class);
        KafkaEnvelopeBinding.injectMetadata(envelope, metadata, Metadata.class);

        assertThat(KafkaEnvelopeBinding.extractPayload(envelope, Payload.class)).isEqualTo(payload);
        assertThat(KafkaEnvelopeBinding.extractMetadata(envelope, Metadata.class)).isEqualTo(metadata);
    }

    @Test
    void shouldInjectUsingSetter() {
        final EnvelopeWithSetter envelope = KafkaEnvelopeBinding.createEnvelope(EnvelopeWithSetter.class);
        final Payload payload = new Payload("user-2");

        KafkaEnvelopeBinding.injectPayload(envelope, payload, Payload.class);

        assertThat(envelope.payload).isEqualTo(payload);
    }

    @Test
    void shouldPreferExactPayloadType() {
        final EnvelopeWithSpecificPayload envelope =
                KafkaEnvelopeBinding.createEnvelope(EnvelopeWithSpecificPayload.class);
        final UserCreatedPayload payload = new UserCreatedPayload("user-5");

        KafkaEnvelopeBinding.injectPayload(envelope, payload, UserCreatedPayload.class);

        assertThat(envelope.getUserCreatedEvent()).isEqualTo(payload);
        assertThat(envelope.getPayload()).isNull();
        assertThat(KafkaEnvelopeBinding.extractPayload(envelope, UserCreatedPayload.class)).isEqualTo(payload);
    }

    @Test
    void shouldRejectAmbiguousPayloadFields() {
        final AmbiguousPayloadEnvelope envelope = KafkaEnvelopeBinding.createEnvelope(AmbiguousPayloadEnvelope.class);
        final Payload payload = new Payload("user-3");

        assertThatThrownBy(() -> KafkaEnvelopeBinding.injectPayload(envelope, payload, Payload.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("multiple payload fields");
    }

    @Test
    void shouldRejectMissingPayloadTarget() {
        final NoPayloadEnvelope envelope = KafkaEnvelopeBinding.createEnvelope(NoPayloadEnvelope.class);
        final Payload payload = new Payload("user-4");

        assertThatThrownBy(() -> KafkaEnvelopeBinding.injectPayload(envelope, payload, Payload.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not expose a payload setter or field");
    }

    @Test
    void shouldRejectMissingNoArgsConstructor() {
        assertThatThrownBy(() -> KafkaEnvelopeBinding.createEnvelope(NoDefaultConstructorEnvelope.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must declare a no-args constructor");
    }

    @Test
    void shouldIgnoreComparableAndEqualsWhenResolvingPayloadSetter() {
        final EnvelopeWithNonSetterMethods envelope =
                KafkaEnvelopeBinding.createEnvelope(EnvelopeWithNonSetterMethods.class);
        final Payload payload = new Payload("user-6");

        KafkaEnvelopeBinding.injectPayload(envelope, payload, Payload.class);

        assertThat(envelope.payload).isEqualTo(payload);
        assertThat(KafkaEnvelopeBinding.extractPayload(envelope, Payload.class)).isEqualTo(payload);
    }

    record Payload(String userId) {
    }

    record Metadata(String traceId) {
    }

    interface BasePayload {
    }

    record UserCreatedPayload(String userId) implements BasePayload {
    }

    static final class EnvelopeWithFields {
        private Payload payload;
        private Metadata metadata;
    }

    static final class EnvelopeWithSetter {
        private Payload payload;

        public void setPayload(final Payload payload) {
            this.payload = payload;
        }
    }

    static final class EnvelopeWithSpecificPayload {
        private BasePayload payload;
        private UserCreatedPayload userCreatedEvent;

        public void setPayload(final BasePayload payload) {
            this.payload = payload;
        }

        public void setUserCreatedEvent(final UserCreatedPayload userCreatedEvent) {
            this.userCreatedEvent = userCreatedEvent;
        }

        public BasePayload getPayload() {
            return this.payload;
        }

        public UserCreatedPayload getUserCreatedEvent() {
            return this.userCreatedEvent;
        }
    }

    static final class AmbiguousPayloadEnvelope {
        private Payload first;
        private Payload second;
    }

    static final class NoPayloadEnvelope {
        private Metadata metadata;
    }

    static final class NoDefaultConstructorEnvelope {
        private final Payload payload;

        NoDefaultConstructorEnvelope(final Payload payload) {
            this.payload = payload;
        }
    }

    static final class EnvelopeWithNonSetterMethods {
        private Payload payload;

        public boolean equals(final Object other) {
            return super.equals(other);
        }

        public int compareTo(final Object other) {
            return 0;
        }
    }
}
