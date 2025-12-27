package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedMetadata;
import com.sitionix.forgeit.consumer.kafka.domain.UserEnvelope;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public final class UserKafkaContracts {

    public static final KafkaContract<UserEnvelope> USER_CREATED_INPUT =
            KafkaContract.producerContract()
                    .topicFromProperty("consumer.kafka.input-topic")
                    .defaultEnvelope(UserEnvelope.class)
                    .defaultPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .defaultMetadata(UserCreatedMetadata.class, "defaultUserCreatedMetadata.json")
                    .build();

    public static final KafkaContract<UserEnvelope> USER_CREATED_OUTPUT =
            KafkaContract.consumerContract()
                    .topic("users.events.out")
                    .defaultEnvelope(UserEnvelope.class)
                    .defaultExpectedPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .defaultMetadata(UserCreatedMetadata.class, "defaultUserCreatedMetadata.json")
                    .build();

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_PAYLOAD_INPUT =
            KafkaContract.producerContract()
                    .topicFromProperty("consumer.kafka.payload-input-topic")
                    .defaultPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .build();

    public static final KafkaContract<UserCreatedEvent> USER_CREATED_PAYLOAD_OUTPUT =
            KafkaContract.consumerContract()
                    .topicFromProperty("consumer.kafka.payload-output-topic")
                    .defaultExpectedPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .build();

    private UserKafkaContracts() {
    }
}
