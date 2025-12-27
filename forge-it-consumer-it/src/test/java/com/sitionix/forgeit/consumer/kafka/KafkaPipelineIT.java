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
    @DisplayName("Given userCreated event When using fixture without mutation Then ForgeIT consumes payload fixture")
    void givenUserCreatedEvent_whenUsingFixtureWithoutMutation_thenForgeItConsumesPayloadFixture() {
        this.support.kafka()
                .publish(UserKafkaContracts.USER_CREATED_INPUT)
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_OUTPUT)
                .assertPayload();
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

    @Test
    @DisplayName("Given payload-only event When using default fixture Then ForgeIT consumes payload")
    void givenPayloadOnlyEvent_whenUsingDefaultFixture_thenForgeItConsumesPayload() {
        final String userId = UUID.randomUUID().toString();

        this.support.kafka()
                .publish(UserKafkaContracts.USER_CREATED_PAYLOAD_INPUT)
                .payload(payload -> payload.setUserId(userId))
                .key(userId)
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_PAYLOAD_OUTPUT)
                .assertPayload(payload -> payload.setUserId(userId));
    }

    @Test
    @DisplayName("Given payload-only event When using override fixture Then ForgeIT consumes payload")
    void givenPayloadOnlyEvent_whenUsingOverrideFixture_thenForgeItConsumesPayload() {
        this.support.kafka()
                .publish(UserKafkaContracts.USER_CREATED_PAYLOAD_INPUT)
                .payload("userCreatedEvent.json")
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_PAYLOAD_OUTPUT)
                .assertPayload("userCreatedEvent.json");
    }

    @Test
    @DisplayName("Given payload-only event When using override fixture and mutation Then ForgeIT consumes payload")
    void givenPayloadOnlyEvent_whenUsingOverrideFixtureAndMutation_thenForgeItConsumesPayload() {
        final String userId = UUID.randomUUID().toString();

        this.support.kafka()
                .publish(UserKafkaContracts.USER_CREATED_PAYLOAD_INPUT)
                .payload("userCreatedEvent.json", payload -> payload.setUserId(userId))
                .key(userId)
                .send();

        this.support.kafka()
                .consume(UserKafkaContracts.USER_CREATED_PAYLOAD_OUTPUT)
                .assertPayload("userCreatedEvent.json", payload -> payload.setUserId(userId));
    }
}
