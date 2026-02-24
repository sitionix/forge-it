package com.sitionix.forgeit.domain.endpoint.mockmvc;

public interface MockmvcDefaultContext {

    MockmvcDefaultContext withRequest(String json);

    MockmvcDefaultContext expectResponse(String json);

    MockmvcDefaultContext expectStatus(int status);

    MockmvcDefaultContext token(String token);

    MockmvcDefaultContext header(String name, String value);

    MockmvcDefaultContext cookie(String name, String value);
}
