package com.sitionix.forgeit.wiremock.internal.journal;

import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.internal.domain.WireMockMappingBuilder;
import org.springframework.stereotype.Component;

@Component
public class WireMockJournal {

    public <Req, Res> WireMockMappingBuilder<Req, Res> createMapping(final Endpoint<Req, Res> endpoint) {
        return new WireMockMappingBuilder<>(endpoint);
    }
}
