package com.sitionix.forgeit.kafka.internal.port;

import com.sitionix.forgeit.kafka.api.KafkaContract;

public interface KafkaPublisherPort {

    <T> void publish(KafkaContract<T> contract, Object payload, String key);
}
