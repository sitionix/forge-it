package com.sitionix.forgeit.wiremock.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class WireMockQueryParams {

    private final Map<String, Parameter> params;

    private WireMockQueryParams() {
        this.params = new LinkedHashMap<>();
    }

    public static WireMockQueryParams create() {
        return new WireMockQueryParams();
    }

    public WireMockQueryParams add(final String name, final Parameter value) {
        if (name == null) {
            throw new IllegalArgumentException("Query parameter name cannot be null");
        }
        if (value == null) {
            return this;
        }
        this.params.put(name, value);
        return this;
    }

    public WireMockQueryParams add(final String name, final String value) {
        return this.add(name, value != null ? Parameter.equalTo(value) : null);
    }

    public WireMockQueryParams add(final String name, final Number value) {
        return this.add(name, value != null ? Parameter.equalTo(String.valueOf(value)) : null);
    }

    public WireMockQueryParams add(final String name, final Boolean value) {
        return this.add(name, value != null ? Parameter.equalTo(String.valueOf(value)) : null);
    }

    public WireMockQueryParams add(final String name, final Enum<?> value) {
        return this.add(name, value != null ? Parameter.equalTo(String.valueOf(value)) : null);
    }

    public WireMockQueryParams add(final String name, final UUID value) {
        return this.add(name, value != null ? Parameter.equalTo(String.valueOf(value)) : null);
    }

    public Map<String, Parameter> asMap() {
        return Collections.unmodifiableMap(this.params);
    }
}
