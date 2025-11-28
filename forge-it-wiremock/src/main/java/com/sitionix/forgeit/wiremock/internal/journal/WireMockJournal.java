package com.sitionix.forgeit.wiremock.internal.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.internal.domain.WireMockMappingBuilder;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service(WireMockJournal.BEAN_NAME)
@RequiredArgsConstructor
public class WireMockJournal {

    public static final String BEAN_NAME = "forgeItWireMockJournal";

    private final WireMockLoaderResources loaderResources;
    private final ObjectMapper objectMapper;

    public <Req, Res> WireMockMappingBuilder<Req, Res> createMapping(final Endpoint<Req, Res> endpoint) {
        return new WireMockMappingBuilder<>(endpoint, this.loaderResources, this.objectMapper);
    }
}
