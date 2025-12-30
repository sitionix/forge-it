package com.sitionix.forgeit.kafka.internal.port;

import com.sitionix.forgeit.kafka.api.KafkaContract;

import java.time.Duration;

public interface KafkaConsumerPort {

    <T> Object consume(KafkaContract<T> contract, Duration timeout);

    <T> Object consumeIfPresent(KafkaContract<T> contract, Duration timeout);
}
