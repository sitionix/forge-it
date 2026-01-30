package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedMetadata;
import com.sitionix.forgeit.consumer.kafka.domain.UserEnvelope;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public final class UserKafkaContracts {

    private static final String CONSUMER_GROUP_ID = "forge-it-consumer-it";

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
                    .groupId(CONSUMER_GROUP_ID)
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
                    .groupId(CONSUMER_GROUP_ID)
                    .defaultExpectedPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                    .build();

    private UserKafkaContracts() {
    }
}
