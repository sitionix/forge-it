package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@IntegrationTest
class KafkaPipelineIT {

    @Autowired
    private KafkaItSupport support;

    @Test
    @DisplayName("Given userCreated event When flowing through user listener and producer Then ForgeIT consumes it")
    void givenUserCreatedEvent_whenFlowingThroughUserListenerAndProducer_thenForgeItConsumesIt() {
        final String userId = UUID.randomUUID().toString();

        this.support.kafka()
                .publish(UserKafkaContracts.USER_CREATED_INPUT)
                .payload("userCreatedEvent.json", payload -> payload.setUserId(userId))
                .key(userId)
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_OUTPUT)
                .expectPayload("userCreatedEvent.json", payload -> payload.setUserId(userId));
    }
}
