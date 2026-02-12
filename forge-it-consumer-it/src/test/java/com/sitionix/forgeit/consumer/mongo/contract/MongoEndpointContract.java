package com.sitionix.forgeit.consumer.mongo.contract;

import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;

public final class MongoEndpointContract {

    private MongoEndpointContract() {
    }

    public static final Endpoint<SomeEntityClass, SomeEntityClass> MONGO_ENTITY_CREATE = Endpoint.createContract(
            "/mongo/entities",
            HttpMethod.POST,
            SomeEntityClass.class,
            SomeEntityClass.class,
            (MockmvcDefault) context -> context
                    .withRequest("mongoCreateRequest.json")
                    .expectStatus(200)
    );
}
