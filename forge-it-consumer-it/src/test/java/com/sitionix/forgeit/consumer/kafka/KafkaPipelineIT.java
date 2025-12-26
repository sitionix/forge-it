package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@IntegrationTest
class KafkaPipelineIT {

    private static final KafkaContract<UserCreatedEvent> USER_CREATED_INPUT =
            KafkaContract.createContract("users.events.in", "UserCreated", UserCreatedEvent.class);

    private static final KafkaContract<UserCreatedEvent> USER_CREATED_OUTPUT =
            KafkaContract.createContract("users.events.out", "UserCreated", UserCreatedEvent.class);

    @Autowired
    private KafkaItSupport support;

    @Test
    @DisplayName("Given userCreated event When flowing through user listener and producer Then ForgeIT consumes it")
    void givenUserCreatedEvent_whenFlowingThroughUserListenerAndProducer_thenForgeItConsumesIt() {
        final String userId = UUID.randomUUID().toString();

        this.support.kafka()
                .publish(USER_CREATED_INPUT)
                .payload("userCreatedEvent.json", payload -> payload.setUserId(userId))
                .key(userId)
                .send();

        this.support.kafka()
                .consume(USER_CREATED_OUTPUT)
                .expectPayload("userCreatedEvent.json", payload -> payload.setUserId(userId));
    }
}
