package com.sitionix.forgeit.wiremock.internal.domain;

import com.sitionix.forgeit.domain.endpoint.Endpoint;

public class WireMockMappingBuilder<Req, Res> {

    public WireMockMappingBuilder(final Endpoint<Req, Res> endpoint) {
        this.endpoint = endpoint;
    }

    private Endpoint<Req, Res> endpoint;


}
