package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public final class UserKafkaContracts {

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_INPUT =
            KafkaContract.builder(UserCreatedEvent.class)
                    .topic("users.events.in")
                    .build();

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_OUTPUT =
            KafkaContract.builder(UserCreatedEvent.class)
                    .topic("users.events.out")
                    .build();

    private UserKafkaContracts() {
    }
}
