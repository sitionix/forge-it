package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedMetadata;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public final class UserKafkaContracts {

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_INPUT =
            KafkaContract.producerContract()
                    .topicFromProperty("consumer.kafka.input-topic")
                    .defaultPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .defaultMetadata(UserCreatedMetadata.class, "defaultUserCreatedMetadata.json")
                    .build();

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_OUTPUT =
            KafkaContract.consumerContract()
                    .topic("users.events.out")
                    .defaultExpectedPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .defaultMetadata(UserCreatedMetadata.class, "defaultUserCreatedMetadata.json")
                    .build();

    private UserKafkaContracts() {
    }
}
