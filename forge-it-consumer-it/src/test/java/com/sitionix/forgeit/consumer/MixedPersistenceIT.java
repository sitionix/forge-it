package com.sitionix.forgeit.consumer;

import com.sitionix.forgeit.consumer.db.contract.EndpointContract;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.consumer.mongo.contract.MongoEndpointContract;
import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@IntegrationTest
class MixedPersistenceIT {

    @Autowired
    private MixedForgeItSupport forgeIt;

    @Test
    void givenMixedSupport_whenPersistingViaMockMvc_thenBothDatabasesAreVerified() {
        this.forgeIt.mockMvc()
                .ping(EndpointContract.USER_CREATE)
                .assertDefault();

        this.forgeIt.mockMvc()
                .ping(MongoEndpointContract.MONGO_ENTITY_CREATE)
                .assertDefault();

        this.forgeIt.postgresql()
                .get(UserEntity.class)
                .hasSize(1)
                .singleElement()
                .andExpected(user -> Objects.equals(user.getUsername(), "testuser"))
                .andExpected(user -> Objects.nonNull(user.getStatus()))
                .andExpected(user -> Objects.equals(user.getStatus().getId(), 1L))
                .assertEntity();

        this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .hasSize(1)
                .singleElement()
                .andExpected(entity -> Objects.equals(entity.getName(), "mockmvc-mongo-name"))
                .andExpected(entity -> Objects.equals(entity.getDescription(), "mockmvc-mongo-description"))
                .assertEntity();
    }
}
