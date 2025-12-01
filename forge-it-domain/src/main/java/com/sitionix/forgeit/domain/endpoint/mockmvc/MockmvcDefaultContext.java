package com.sitionix.forgeit.domain.endpoint.mockmvc;

public interface MockmvcDefaultContext {

    MockmvcDefaultContext request(String json);

    MockmvcDefaultContext response(String json);

    MockmvcDefaultContext status(int status);
}
