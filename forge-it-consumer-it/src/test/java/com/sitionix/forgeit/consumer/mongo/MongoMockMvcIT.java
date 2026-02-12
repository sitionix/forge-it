package com.sitionix.forgeit.consumer.mongo;

import com.sitionix.forgeit.consumer.mongo.contract.MongoEndpointContract;
import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@IntegrationTest
class MongoMockMvcIT {

    @Autowired
    private MongoMockMvcForgeItSupport forgeIt;

    @Test
    void givenMockMvcRequest_whenCreatingMongoEntity_thenEntityIsPersisted() {
        this.forgeIt.mockMvc()
                .ping(MongoEndpointContract.MONGO_ENTITY_CREATE)
                .assertDefault();

        this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .hasSize(1)
                .singleElement()
                .andExpected(entity -> Objects.equals(entity.getName(), "mockmvc-mongo-name"))
                .andExpected(entity -> Objects.equals(entity.getDescription(), "mockmvc-mongo-description"))
                .assertEntity();
    }
}
