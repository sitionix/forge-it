package com.sitionix.forgeit.wiremock.internal.journal;

import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.internal.domain.WireMockMappingBuilder;
import org.springframework.stereotype.Service;

@Service(WireMockJournal.BEAN_NAME)
public class WireMockJournal {

    public static final String BEAN_NAME = "forgeItWireMockJournal";

    public <Req, Res> WireMockMappingBuilder<Req, Res> createMapping(final Endpoint<Req, Res> endpoint) {
        return new WireMockMappingBuilder<>(endpoint);
    }
}
