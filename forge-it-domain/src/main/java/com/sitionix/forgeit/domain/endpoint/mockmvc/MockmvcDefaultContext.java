package com.sitionix.forgeit.domain.endpoint.mockmvc;

public interface MockmvcDefaultContext {

    MockmvcDefaultContext withRequest(String json);

    MockmvcDefaultContext expectResponse(String json);

    MockmvcDefaultContext expectStatus(int status);

    MockmvcDefaultContext token(String token);
}
