package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public final class UserKafkaContracts {

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_INPUT =
            KafkaContract.payloadClass(UserCreatedEvent.class)
                    .topicFromProperty("consumer.kafka.input-topic")
                    .build();

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_OUTPUT =
            KafkaContract.payloadClass(UserCreatedEvent.class)
                    .topic("users.events.out")
                    .build();

    private UserKafkaContracts() {
    }
}
