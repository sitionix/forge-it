package com.sitionix.forgeit.wiremock.internal;

import com.sitionix.forgeit.wiremock.internal.WireMockSupportBridge.WireMockDelegate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.URI;

@RequiredArgsConstructor
public final class WireMockFacade implements InitializingBean, DisposableBean, WireMockDelegate {

    static final String BEAN_NAME = "forgeItWireMockFacade";

    private final WireMockContainerManager containerManager;

    @Override
    public void afterPropertiesSet() {
        WireMockSupportBridge.setDelegate(this);
    }

    @Override
    public void destroy() {
        WireMockSupportBridge.clearDelegate();
    }

    @Override
    public String wiremock() {
        final URI baseUrl = this.containerManager.getBaseUrl();
        return baseUrl.toString();
    }
}
