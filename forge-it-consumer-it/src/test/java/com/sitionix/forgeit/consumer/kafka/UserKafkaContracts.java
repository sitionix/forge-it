package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public final class UserKafkaContracts {

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_INPUT =
            KafkaContract.producerContract(UserCreatedEvent.class)
                    .topicFromProperty("consumer.kafka.input-topic")
                    .defaultPayload("defaultUserCreatedEvent.json")
                    .build();

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_OUTPUT =
            KafkaContract.consumerContract(UserCreatedEvent.class)
                    .topic("users.events.out")
                    .defaultExpectedPayload("defaultUserCreatedEvent.json")
                    .build();

    private UserKafkaContracts() {
    }
}
