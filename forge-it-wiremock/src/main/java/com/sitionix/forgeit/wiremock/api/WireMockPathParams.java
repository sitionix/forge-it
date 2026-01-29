package com.sitionix.forgeit.wiremock.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WireMockPathParams {

    private final Map<String, Object> params;

    private WireMockPathParams() {
        this.params = new LinkedHashMap<>();
    }

    public static WireMockPathParams create() {
        return new WireMockPathParams();
    }

    public WireMockPathParams add(final String name, final String value) {
        return this.addInternal(name, value);
    }

    public WireMockPathParams add(final String name, final Number value) {
        return this.addInternal(name, value);
    }

    public WireMockPathParams add(final String name, final Boolean value) {
        return this.addInternal(name, value);
    }

    public WireMockPathParams add(final String name, final Parameter value) {
        return this.addInternal(name, value);
    }

    private WireMockPathParams addInternal(final String name, final Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Path parameter name cannot be null");
        }
        if (value == null) {
            return this;
        }
        this.params.put(name, value);
        return this;
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.params);
    }
}
