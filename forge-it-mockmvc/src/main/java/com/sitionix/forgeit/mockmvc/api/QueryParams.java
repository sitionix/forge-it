package com.sitionix.forgeit.mockmvc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QueryParams {

    private final Map<String, Object> params;

    private QueryParams() {
        this.params = new LinkedHashMap<>();
    }

    public static QueryParams create() {
        return new QueryParams();
    }

    public QueryParams add(final String name, final Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Query parameter name cannot be null");
        }
        if (value == null) {
            return this;
        }

        final Object existing = this.params.get(name);
        if (existing == null) {
            this.params.put(name, value);
            return this;
        }

        if (existing instanceof List<?> existingList) {
            final List<Object> values = new ArrayList<>(existingList.size() + 1);
            values.addAll(existingList);
            values.add(value);
            this.params.put(name, values);
            return this;
        }

        final List<Object> values = new ArrayList<>();
        values.add(existing);
        values.add(value);
        this.params.put(name, values);
        return this;
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.params);
    }
}
