package com.sitionix.forgeit.wiremock.internal.domain;

import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.api.WireMockPathParams;
import com.sitionix.forgeit.wiremock.internal.configs.PathTemplate;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

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

    public RequestBuilder<Req, Res> pathWithParameters(final WireMockPathParams parameters) {
        if (nonNull(parameters) && !parameters.asMap().isEmpty()) {
            this.endpoint.getUrlBuilder().applyParameters(parameters.asMap(), PathTemplate::withPathParams);
        }
        return this;
    }

    public RequestBuilder<Req, Res> jsonName(final String jsonName) {
        if (nonNull(jsonName)) {
            this.jsonValue = this.loaderResources.mappingRequest().getFromFile(jsonName);
        }
        return this;
    }

    public RequestBuilder<Req, Res> json(final String jsonValue) {
        if (nonNull(jsonValue)) {
            this.jsonValue = jsonValue;
        }
        return this;
    }

    public RequestBuilder<Req, Res> ignoringFields(final String... ignoringFields) {
        if (nonNull(ignoringFields)) {
            Collections.addAll(this.ignoringFields, ignoringFields);
        }
        return this;
    }

    public RequestBuilder<Req, Res> id(final UUID id) {
        if (nonNull(id)) {
            this.id = id;
        }
        return this;
    }

    public WireMockCheck<Req, Res> build() {
        return new WireMockCheck<>(
                this.endpoint,
                this.jsonValue,
                this.atLeastTimes,
                this.ignoringFields,
                this.id
        );
    }

    public void verify() {
        this.verifier.accept(this.build());
    }
}
