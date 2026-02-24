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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MockMvcBuilderCookiesTest {

    @Test
    void shouldApplyDefaultCookieFromEndpointDefaults() {
        final MockMvcBuilder<String, String> builder = createBuilder(context -> context.cookie("refresh", "default"));

        builder.expectStatus(HttpStatus.OK)
                .andExpectPath(jsonPath("$.refresh").value("default"))
                .assertAndCreate();
    }

    @Test
    void shouldOverrideDefaultCookieWithExplicitCookie() {
        final MockMvcBuilder<String, String> builder = createBuilder(context -> context.cookie("refresh", "default"));

        builder.cookie("refresh", "override")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(jsonPath("$.refresh").value("override"))
                .assertAndCreate();
    }

    @Test
    void shouldAllowDisablingCookieBySettingExplicitNullCookie() {
        final MockMvcBuilder<String, String> builder = createBuilder(context -> context.cookie("refresh", "default"));

        builder.cookie("refresh", null)
                .expectStatus(HttpStatus.OK)
                .andExpectPath(jsonPath("$.refresh").doesNotExist())
                .assertAndCreate();
    }

    private static MockMvcBuilder<String, String> createBuilder(final MockmvcDefault defaults) {
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CookieEchoController()).build();
        final MockMvcLoader mockMvcLoader = new MockMvcLoader(new FakeJsonLoader(), getProperties());
        final Endpoint<String, String> endpoint = defaults == null
                ? Endpoint.createContract("/cookies", HttpMethod.GET, String.class, String.class)
                : Endpoint.createContract("/cookies", HttpMethod.GET, String.class, String.class, defaults);

        return new MockMvcBuilder<>(mockMvc, mockMvcLoader, new ObjectMapper(), endpoint);
    }

    private static MockMvcProperties getProperties() {
        final MockMvcProperties properties = new MockMvcProperties();
        final MockMvcProperties.Path path = new MockMvcProperties.Path();
        path.setRequest("/mockmvc/request");
        path.setResponse("/mockmvc/response");
        path.setDefaultRequest("/mockmvc/default/request");
        path.setDefaultResponse("/mockmvc/default/response");
        properties.setPath(path);
        properties.setDefaultHeaders(new LinkedHashMap<>());
        return properties;
    }

    static final class FakeJsonLoader implements JsonLoader {
        private final Map<String, String> data = new HashMap<>();

        @Override
        public <T> T getFromFile(final String fileName, final Class<T> tClass) {
            throw new IllegalStateException("Object fixtures are not configured for this test");
        }

        @Override
        public String getFromFile(final String fileName) {
            final String value = this.data.get(fileName);
            if (value == null) {
                throw new IllegalStateException("Fixture not found: " + fileName);
            }
            return value;
        }

        @Override
        public void setBasePath(final String basePath) {
            // no-op for cookie-only tests
        }
    }

    @RestController
    static final class CookieEchoController {

        @GetMapping(path = "/cookies", produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<Map<String, String>> echo(@CookieValue(value = "refresh", required = false) final String refresh) {
            final Map<String, String> response = new LinkedHashMap<>();
            if (refresh != null) {
                response.put("refresh", refresh);
            }
            return ResponseEntity.ok(response);
        }
    }
}
