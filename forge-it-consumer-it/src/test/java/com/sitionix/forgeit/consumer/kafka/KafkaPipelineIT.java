package com.sitionix.forgeit.consumer.kafka;

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
                .payload("userCreatedEvent.json", envelope -> envelope.getPayload().setUserId(userId))
                .metadata(envelope -> envelope.getMetadata().setTraceId("t-" + userId))
                .key(userId)
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_OUTPUT)
                .assertPayload("userCreatedEvent.json", envelope -> envelope.getPayload().setUserId(userId))
                .assertMetadata(envelope -> envelope.getMetadata().setTraceId("t-" + userId));
    }

    @Test
    @DisplayName("Given default userCreated event When flowing through user listener and producer Then ForgeIT consumes default fixture")
    void givenDefaultUserCreatedEvent_whenFlowingThroughUserListenerAndProducer_thenForgeItConsumesDefaultFixture() {
        final String userId = UUID.randomUUID().toString();

        this.support.kafka()
                .publish(UserKafkaContracts.USER_CREATED_INPUT)
                .payload(envelope -> envelope.getPayload().setUserId(userId))
                .metadata(envelope -> envelope.getMetadata().setTraceId("t-" + userId))
                .key(userId)
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_OUTPUT)
                .assertPayload(envelope -> envelope.getPayload().setUserId(userId))
                .assertMetadata(envelope -> envelope.getMetadata().setTraceId("t-" + userId));
    }
}
