package com.sitionix.forgeit.mockmvc.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PathParams {

    private final Map<String, Object> params;

    private PathParams() {
        this.params = new LinkedHashMap<>();
    }

    public static PathParams create() {
        return new PathParams();
    }

    public PathParams add(final String name, final Object value) {
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
