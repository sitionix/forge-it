package com.sitionix.forgeit.mockmvc.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.journal.MockMvcJournal;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MockMvcJournalDefaultHeadersTest {

    @Test
    void shouldApplyDefaultHeadersFromProperties() {
        final MockMvcJournal journal = getJournal(Map.of(
                "x-test", "from-properties",
                HttpHeaders.AUTHORIZATION, "Bearer properties-auth"
        ));

        journal.ping(Endpoint.createContract("/headers", HttpMethod.GET, String.class, String.class))
                .expectStatus(HttpStatus.OK)
                .andExpectPath(jsonPath("$.xTest").value("from-properties"))
                .andExpectPath(jsonPath("$.authorization").value("Bearer properties-auth"))
                .assertAndCreate();
    }

    @Test
    void shouldAllowOverridingDefaultHeadersFromProperties() {
        final MockMvcJournal journal = getJournal(Map.of(
                "x-test", "from-properties",
                HttpHeaders.AUTHORIZATION, "Bearer properties-auth"
        ));

        journal.ping(Endpoint.createContract("/headers", HttpMethod.GET, String.class, String.class))
                .header("x-test", "from-explicit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer explicit-auth")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(jsonPath("$.xTest").value("from-explicit"))
                .andExpectPath(jsonPath("$.authorization").value("Bearer explicit-auth"))
                .assertAndCreate();
    }

    private static MockMvcJournal getJournal(final Map<String, String> defaultHeaders) {
        final MockMvcProperties properties = new MockMvcProperties();
        final MockMvcProperties.Path path = new MockMvcProperties.Path();
        path.setRequest("/mockmvc/request");
        path.setResponse("/mockmvc/response");
        path.setDefaultRequest("/mockmvc/default/request");
        path.setDefaultResponse("/mockmvc/default/response");
        properties.setPath(path);
        properties.setDefaultHeaders(new LinkedHashMap<>(defaultHeaders));

        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HeaderEchoController()).build();
        return new MockMvcJournal(
                new ObjectMapper(),
                new MockMvcLoader(new FakeJsonLoader(), properties),
                mockMvc,
                properties
        );
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
            // no-op for header-only tests
        }
    }

    @RestController
    static final class HeaderEchoController {

        @GetMapping(path = "/headers", produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<Map<String, String>> echo(
                @RequestHeader(value = "x-test", required = false) final String xTest,
                @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authorization
        ) {
            final Map<String, String> response = new LinkedHashMap<>();
            if (xTest != null) {
                response.put("xTest", xTest);
            }
            if (authorization != null) {
                response.put("authorization", authorization);
            }
            return ResponseEntity.ok(response);
        }
    }
}
