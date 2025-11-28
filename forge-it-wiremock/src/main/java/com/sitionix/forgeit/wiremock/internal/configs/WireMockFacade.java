package com.sitionix.forgeit.wiremock.internal.configs;

import com.sitionix.forgeit.wiremock.internal.configs.WireMockSupportBridge.WireMockDelegate;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;

@RequiredArgsConstructor
@Service(value = WireMockFacade.BEAN_NAME)
public final class WireMockFacade implements WireMockDelegate {

    static final String BEAN_NAME = "forgeItWireMockFacade";

    private final WireMockJournal wireMockJournal;

    @Override
    public WireMockJournal wiremock() {
       return this.wireMockJournal;
    }
}
