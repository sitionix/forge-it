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

    public WireMockQueryParams add(final String name, final Object value) {
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
