package com.sitionix.forgeit.consumer.db.contract;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;

public class EndpointContract {

    public static final Endpoint<UserEntity, UserEntity> USER_CREATE = Endpoint.createContract(
            "/users/register",
            HttpMethod.POST,
            UserEntity.class,
            UserEntity.class,
            (MockmvcDefault) context -> context
                    .withRequest("userCreateRequest.json")
                    .expectStatus(200)
    );
}
