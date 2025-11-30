package com.sitionix.forgeit.wiremock.internal.domain;

import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RequestBuilder<Req, Res> {

    public RequestBuilder(final WireMockLoaderResources loaderResources,
                          final Consumer<WireMockCheck<Req, Res>> verifier,
                          final Endpoint<Req, Res> endpoint) {
        this.loaderResources = loaderResources;
        this.verifier = verifier;
        this.endpoint = endpoint;
        this.ignoringFields = new ArrayList<>();
        this.atLeastTimes = 1;
    }

    private final WireMockLoaderResources loaderResources;

    private final Consumer<WireMockCheck<Req, Res>> verifier;

    private final List<String> ignoringFields;
    
    private int atLeastTimes;

    private Endpoint<Req, Res> endpoint;
    
    private String jsonValue;
    
    @Getter
    private UUID id;
    
    public RequestBuilder<Req, Res> atLeastTimes(final int atLeastTimes) {
        this.atLeastTimes = atLeastTimes;
        return this;
    }

    public RequestBuilder<Req, Res> pathWithParameters(Map<String, Object> parameters) {

        return this;
    }

}
