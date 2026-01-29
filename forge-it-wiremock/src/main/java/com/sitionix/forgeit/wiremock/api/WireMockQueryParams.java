package com.sitionix.forgeit.wiremock.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WireMockQueryParams {

    private final Map<String, Object> params;

    private WireMockQueryParams() {
        this.params = new LinkedHashMap<>();
    }

    public static WireMockQueryParams create() {
        return new WireMockQueryParams();
    }

    public WireMockQueryParams add(final String name, final String value) {
        return this.addInternal(name, value);
    }

    public WireMockQueryParams add(final String name, final Number value) {
        return this.addInternal(name, value);
    }

    public WireMockQueryParams add(final String name, final Boolean value) {
        return this.addInternal(name, value);
    }

    public WireMockQueryParams add(final String name, final Parameter value) {
        return this.addInternal(name, value);
    }

    @Deprecated(forRemoval = true)
    public WireMockQueryParams add(final String name, final Object value) {
        if (value == null) {
            return this.addInternal(name, null);
        }
        if (value instanceof String stringValue) {
            return this.add(name, stringValue);
        }
        if (value instanceof Number numberValue) {
            return this.add(name, numberValue);
        }
        if (value instanceof Boolean booleanValue) {
            return this.add(name, booleanValue);
        }
        if (value instanceof Parameter parameterValue) {
            return this.add(name, parameterValue);
        }
        throw new IllegalArgumentException("Unsupported query parameter value type: " + value.getClass().getName());
    }

    private WireMockQueryParams addInternal(final String name, final Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Query parameter name cannot be null");
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
