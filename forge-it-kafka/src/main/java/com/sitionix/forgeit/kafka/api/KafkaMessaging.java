package com.sitionix.forgeit.kafka.api;

/**
 * Public contract describing Kafka messaging capabilities exposed to ForgeIT clients.
 */
public interface KafkaMessaging {

    <T> KafkaPublishBuilder<T> publish(KafkaContract<T> contract);

    <T> KafkaConsumeBuilder<T> consume(KafkaContract<T> contract);
}
