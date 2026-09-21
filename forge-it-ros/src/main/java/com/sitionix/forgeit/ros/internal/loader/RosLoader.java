package com.sitionix.forgeit.ros.internal.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.Function;

public final class RosLoader {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Function<String, String> fixtures;

    public RosLoader(final Function<String, String> fixtures) {
        this.fixtures = fixtures;
    }

    public JsonNode payload(final String file) {
        return this.load("/ros/publish", file);
    }

    public JsonNode defaultPayload(final String file) {
        return this.load("/ros/default/publish", file);
    }

    public JsonNode expectedPayload(final String file) {
        return this.load("/ros/expected", file);
    }

    public JsonNode defaultExpectedPayload(final String file) {
        return this.load("/ros/default/expected", file);
    }

    private JsonNode load(final String folder, final String file) {
        if (file == null || file.isBlank() || file.contains("..") || !file.matches("[A-Za-z0-9_/-]+\\.json")) {
            throw new IllegalArgumentException("ROS fixture must be a relative JSON resource name");
        }
        try {
            final JsonNode result = JSON.readTree(this.fixtures.apply(folder + "/" + file));
            if (result == null || !result.isObject()) {
                throw new IllegalArgumentException();
            }
            return result;
        } catch (final RuntimeException | IOException ex) {
            throw new IllegalArgumentException("ROS fixture could not be loaded as a JSON object");
        }
    }
}
