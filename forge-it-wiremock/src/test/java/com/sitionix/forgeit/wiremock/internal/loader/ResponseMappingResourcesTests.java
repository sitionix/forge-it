package com.sitionix.forgeit.wiremock.internal.loader;

import com.sitionix.forgeit.wiremock.internal.WireMockProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = ResponseMappingResourcesTests.TestApplication.class,
    properties = "forge-it.modules.wiremock.mapping.response=mappings/responses"
)
class ResponseMappingResourcesTests {

    private static final String EXPECTED_JSON = """
        {
          "status": "ok",
          "payload": {
            "message": "Response mapping works"
          }
        }
        """;

    @Autowired
    private ResponseMappingResources responseMappingResources;

    @Test
    void shouldLoadResponseMappingAsRawJson() {
        final String rawJson = responseMappingResources.getFromFile("sample-response.json");

        assertThat(rawJson).isEqualTo(EXPECTED_JSON);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(WireMockProperties.class)
    @ComponentScan("com.sitionix.forgeit.wiremock.internal")
    static class TestApplication {
    }
}
