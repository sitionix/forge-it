package com.sitionix.forgeit.wiremock.internal.domain;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.testing.MockExtension;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WireMockMappingBuilderTests extends MockExtension {

    private static final Endpoint<SampleRequest, SampleResponse> ENDPOINT = Endpoint.createContract(
            "/test/endpoint",
            HttpMethod.POST,
            SampleRequest.class,
            SampleResponse.class
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WireMockLoaderResources loaderResources;

    @Mock
    private ResourcesLoader requestLoader;

    @Mock
    private ResourcesLoader responseLoader;

    @Mock
    private WireMock wireMockClient;

    @Test
    void shouldRegisterMappingUsingWireMockAdminClient() {
        when(this.loaderResources.mappingRequest()).thenReturn(this.requestLoader);
        when(this.loaderResources.mappingResponse()).thenReturn(this.responseLoader);
        when(this.requestLoader.getFromFile(anyString())).thenReturn("{\"query\":true}");
        when(this.responseLoader.getFromFile(anyString())).thenReturn("{\"result\":\"ok\"}");

        final WireMockMappingBuilder<SampleRequest, SampleResponse> builder = new WireMockMappingBuilder<>(
                ENDPOINT,
                this.loaderResources,
                this.objectMapper,
                this.wireMockClient
        );

        final StubMapping actualMapping = builder
                .matchesJson("request.json")
                .responseBody("response.json")
                .responseStatus(HttpStatus.ACCEPTED)
                .plainUrl()
                .create()
                .build();

        final ArgumentCaptor<MappingBuilder> mappingCaptor = ArgumentCaptor.forClass(MappingBuilder.class);
        verify(this.wireMockClient).register(mappingCaptor.capture());

        final StubMapping capturedMapping = mappingCaptor.getValue().build();

        assertThat(actualMapping.getRequest().getMethod().getName()).isEqualTo("POST");
        assertThat(actualMapping.getRequest().getUrl()).isEqualTo("/test/endpoint");
        assertThat(actualMapping.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(actualMapping.getResponse().getBody()).isEqualTo("{\"result\":\"ok\"}");

        assertThat(capturedMapping.getRequest().getMethod().getName()).isEqualTo("POST");
        assertThat(capturedMapping.getRequest().getUrl()).isEqualTo("/test/endpoint");
        assertThat(capturedMapping.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
    }

    private record SampleRequest(boolean query) {
    }

    private record SampleResponse(String result) {
    }
}
