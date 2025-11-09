package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@IntegrationTest(classes = ForgeItIntegrationTest.TestApplication.class)
class ForgeItIntegrationTest {

    @Autowired
    private UserInterface tools;

    @Test
    void shouldExposeWireMockFeature() throws Exception {
        final var wireMock = tools.wiremock();
        wireMock.stubFor(get(urlEqualTo("/greeting"))
                .willReturn(aResponse().withStatus(200).withBody("hello")));

        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(wireMock.baseUrl().resolve("/greeting"))
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.body()).isEqualTo("hello");
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
