package com.sitionix.forgeit.wiremock.internal;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

final class WireMockFacade implements InitializingBean, DisposableBean, WireMockSupportBridge.WireMockDelegate {

    static final String BEAN_NAME = "forgeItWireMockFacade";

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
        return "wiremock";
    }
}
