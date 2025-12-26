package com.sitionix.forgeit.kafka.api;

/**
 * Port interface exposing Kafka interactions to ForgeIT clients.
 */
public interface KafkaBridge {

    /**
     * Emit a lightweight log entry to confirm the Kafka bridge is active.
     */
    void bridge();
}
