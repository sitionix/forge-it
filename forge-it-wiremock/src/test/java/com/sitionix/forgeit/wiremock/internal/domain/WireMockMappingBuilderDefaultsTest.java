package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.wiremock.WiremockDefault;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.wiremock.internal.configs.WireMockProperties;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockMappingBuilderDefaultsTest {

    @Test
    void shouldSkipMissingDefaultFixtures() {
        final FakeJsonLoader loader = new FakeJsonLoader();
        final WireMockLoaderResources resources = new WireMockLoaderResources(createProperties(), loader);
        final WiremockDefault defaults = context -> context
                .matchesJson(" ")
                .responseBody(" ");

        final Endpoint<String, String> endpoint =
                Endpoint.createContract("/test", HttpMethod.GET, String.class, String.class, defaults);
        final WireMockMappingBuilder<String, String> builder = new WireMockMappingBuilder<>(endpoint,
                resources,
                new ObjectMapper(),
                null,
                null);

        builder.applyDefault(context -> {
        });

        assertThat(builder.getRequestJson()).isNull();
        assertThat(builder.getResponseJson()).isNull();
    }

    @Test
    void shouldLoadCustomFixturesWhenDefaultsMissing() {
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/wiremock/request/customRequest.json", "{\"name\":\"request\"}");
        loader.put("/wiremock/response/customResponse.json", "{\"status\":\"ok\"}");
        final WireMockLoaderResources resources = new WireMockLoaderResources(createProperties(), loader);

        final Endpoint<String, String> endpoint =
                Endpoint.createContract("/test", HttpMethod.GET, String.class, String.class);
        final WireMockMappingBuilder<String, String> builder = new WireMockMappingBuilder<>(endpoint,
                resources,
                new ObjectMapper(),
                null,
                null);

        builder.matchesJson("customRequest.json")
                .responseBody("customResponse.json");

        assertThat(builder.getRequestJson()).isEqualTo("{\"name\":\"request\"}");
        assertThat(builder.getResponseJson()).isEqualTo("{\"status\":\"ok\"}");
    }

    private static WireMockProperties createProperties() {
        final WireMockProperties properties = new WireMockProperties();
        final WireMockProperties.Mapping mapping = new WireMockProperties.Mapping();
        mapping.setRequest("/wiremock/request");
        mapping.setResponse("/wiremock/response");
        mapping.setDefaultRequest("/wiremock/default/request");
        mapping.setDefaultResponse("/wiremock/default/response");
        properties.setMapping(mapping);
        return properties;
    }

    static final class FakeJsonLoader implements JsonLoader {
        private final Map<String, String> data = new HashMap<>();
        private String basePath;

        void put(final String path, final String json) {
            this.data.put(path, json);
        }

        @Override
        public <T> T getFromFile(final String fileName, final Class<T> tClass) {
            throw new IllegalStateException("Object fixtures are not configured for this test");
        }

        @Override
        public String getFromFile(final String fileName) {
            final String value = this.data.get(this.basePath + "/" + fileName);
            if (value == null) {
                throw new IllegalStateException("Fixture not found: " + this.basePath + "/" + fileName);
            }
            return value;
        }

        @Override
        public void setBasePath(final String basePath) {
            this.basePath = basePath;
        }
    }
}
