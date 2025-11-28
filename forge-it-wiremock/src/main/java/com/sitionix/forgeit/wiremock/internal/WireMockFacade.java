package com.sitionix.forgeit.wiremock.internal;

import com.sitionix.forgeit.wiremock.internal.WireMockSupportBridge.WireMockDelegate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.net.URI;

@RequiredArgsConstructor
@Service(value = WireMockFacade.BEAN_NAME)
public final class WireMockFacade implements WireMockDelegate {

    static final String BEAN_NAME = "forgeItWireMockFacade";

    private final WireMockContainerManager containerManager;

    @Override
    public String wiremock() {
        final URI baseUrl = this.containerManager.getBaseUrl();
        return baseUrl.toString();
    }
}
