package com.sitionix.forgeit.wiremock.internal.configs;

import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;

import java.util.Objects;

/**
 * Internal delegation point for WireMock infrastructure interactions.
 * <p>
 * This type intentionally lives outside of the public API package so that
 * feature logic remains encapsulated and can evolve without breaking
 * consumers. Only {@code com.sitionix.forgeit.wiremock.api} should interact
 * with this class.
 */
public final class WireMockSupportBridge {

    private static final WireMockDelegate UNINITIALISED = () -> {
        throw new IllegalStateException("WireMock feature has not been initialised");
    };

    private static final WireMockDelegate SHUTDOWN = () -> {
        throw new IllegalStateException("WireMock feature has been shut down");
    };

    private static volatile WireMockDelegate delegate = UNINITIALISED;

    private WireMockSupportBridge() {
    }

    public static WireMockJournal wiremock() {
        return delegate.wiremock();
    }

    public static void setDelegate(WireMockDelegate delegate) {
        WireMockSupportBridge.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static void clearDelegate() {
        WireMockSupportBridge.delegate = SHUTDOWN;
    }

    @FunctionalInterface
    public interface WireMockDelegate {
        WireMockJournal wiremock();
    }
}
