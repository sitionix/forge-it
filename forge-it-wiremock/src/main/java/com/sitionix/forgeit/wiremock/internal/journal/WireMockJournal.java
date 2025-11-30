package com.sitionix.forgeit.wiremock.internal.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.internal.domain.RequestBuilder;
import com.sitionix.forgeit.wiremock.internal.domain.WireMockCheck;
import com.sitionix.forgeit.wiremock.internal.domain.WireMockMappingBuilder;
import com.sitionix.forgeit.wiremock.internal.configs.WireMockContainerManager;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WireMockJournal {

    private final WireMockLoaderResources loaderResources;
    private final ObjectMapper objectMapper;
    private final WireMockContainerManager containerManager;

    public <Req, Res> WireMockMappingBuilder<Req, Res> createMapping(final Endpoint<Req, Res> endpoint) {
        return new WireMockMappingBuilder<>(endpoint, this.loaderResources, this.objectMapper,
                this.containerManager.getClient());
    }

    public <Req, Res> RequestBuilder<Req, Res> check(final Endpoint<Req, Res> endpoint) {
        return new RequestBuilder<>(this.loaderResources, this::verify, endpoint);
    }

    private <Req, Res> void verify(final WireMockCheck<Req, Res> check) {

    }
}
