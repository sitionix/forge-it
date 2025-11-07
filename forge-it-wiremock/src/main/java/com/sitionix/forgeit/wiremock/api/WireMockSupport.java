package com.sitionix.forgeit.wiremock.api;

import com.sitionix.forgeit.wiremock.internal.WireMockSupportBridge;

/**
 * Public contract describing WireMock capabilities exposed to ForgeIT clients.
 * <p>
 * All behaviour is funneled through internal bridges so that consumers remain
 * isolated from infrastructure details.
 */
public interface WireMockSupport {

    default String wiremock() {
        return WireMockSupportBridge.wiremock();
    }
}
