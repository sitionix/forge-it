package com.sitionix.forgeit.mockmvc.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MockMvcBuilderDefaultsTest {

    @Test
    void shouldSkipMissingDefaultFixtures() {
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
        final FakeJsonLoader loader = new FakeJsonLoader();
        final MockMvcBuilder<String, String> builder = createBuilder(mockMvc,
                loader,
                context -> context
                        .withRequest(" ")
                        .expectResponse(" ")
                        .expectStatus(200));

        assertDoesNotThrow(() -> builder.assertDefault());
    }

    @Test
    void shouldUseCustomFixturesWhenDefaultsMissing() {
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/mockmvc/request/customRequest.json", "{\"name\":\"request\"}");
        loader.put("/mockmvc/response/customResponse.json", "{\"status\":\"ok\"}");

        final MockMvcBuilder<String, String> builder = createBuilder(mockMvc, loader, null);

        assertDoesNotThrow(() -> builder.withRequest("customRequest.json")
                .expectResponse("customResponse.json")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate());
    }

    private static MockMvcBuilder<String, String> createBuilder(final MockMvc mockMvc,
                                                                final FakeJsonLoader loader,
                                                                final MockmvcDefault defaults) {
        final MockMvcProperties properties = new MockMvcProperties();
        final MockMvcProperties.Path path = new MockMvcProperties.Path();
        path.setRequest("/mockmvc/request");
        path.setResponse("/mockmvc/response");
        path.setDefaultRequest("/mockmvc/default/request");
        path.setDefaultResponse("/mockmvc/default/response");
        properties.setPath(path);

        final MockMvcLoader mockMvcLoader = new MockMvcLoader(loader, properties);
        final Endpoint<String, String> endpoint = defaults != null
                ? Endpoint.createContract("/test", HttpMethod.POST, String.class, String.class, defaults)
                : Endpoint.createContract("/test", HttpMethod.POST, String.class, String.class);

        return new MockMvcBuilder<>(mockMvc, mockMvcLoader, new ObjectMapper(), endpoint);
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

    @RestController
    static final class TestController {

        @PostMapping(path = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<String> handle(@RequestBody(required = false) final String body) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":\"ok\"}");
        }
    }
}
