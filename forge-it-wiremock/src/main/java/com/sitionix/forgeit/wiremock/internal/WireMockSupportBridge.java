package com.sitionix.forgeit.wiremock.internal;

/**
 * Internal delegation point for WireMock infrastructure interactions.
 * <p>
 * This type intentionally lives outside of the public API package so that
 * feature logic remains encapsulated and can evolve without breaking
 * consumers. Only {@code com.sitionix.forgeit.wiremock.api} should interact
 * with this class.
 */
public final class WireMockSupportBridge {

    private WireMockSupportBridge() {
    }

    public static String wiremock() {
        return "wiremock";
    }
}
