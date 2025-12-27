package com.sitionix.forgeit.kafka.api;

/**
 * Describes a default Kafka envelope payload fixture.
 */
public record DefaultEnvelope(Class<?> envelopeType, String envelopeName) {

    public DefaultEnvelope {
        if (envelopeType == null) {
            throw new IllegalArgumentException("envelopeType must be provided");
        }
        if (envelopeName == null || envelopeName.isBlank()) {
            throw new IllegalArgumentException("envelopeName must be provided");
        }
    }
}
