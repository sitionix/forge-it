package com.sitionix.forgeit.domain.endpoint.mockmvc;

public interface MockmvcDefaultContext {

    MockmvcDefaultContext withRequest(String json);

    MockmvcDefaultContext expectResponse(String json);

    default MockmvcDefaultContext expectResponse(final String json, final boolean strict) {
        return expectResponse(json);
    }

    MockmvcDefaultContext expectStatus(int status);
}
