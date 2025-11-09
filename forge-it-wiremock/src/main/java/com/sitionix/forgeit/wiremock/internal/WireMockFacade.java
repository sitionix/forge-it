package com.sitionix.forgeit.wiremock.internal;

import com.sitionix.forgeit.wiremock.internal.WireMockSupportBridge.WireMockDelegate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.URI;

public final class WireMockFacade implements InitializingBean, DisposableBean, WireMockDelegate {

    static final String BEAN_NAME = "forgeItWireMockFacade";

    private final WireMockContainerManager containerManager;

    public WireMockFacade(WireMockContainerManager containerManager) {
        this.containerManager = containerManager;
    }

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
        final URI baseUrl = this.containerManager.baseUrl();
        return baseUrl.toString();
    }
}
