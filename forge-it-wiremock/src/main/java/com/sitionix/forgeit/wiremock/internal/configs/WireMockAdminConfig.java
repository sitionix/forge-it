package com.sitionix.forgeit.wiremock.internal.configs;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Configuration
public class WireMockAdminConfig {

    @Bean
    @DependsOn("wireMockContainerManager")
    public RestClient wireMockAdminClient(WireMockContainerManager wireMockContainerManager) {
        final URI baseUrl = wireMockContainerManager.getBaseUrl();
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .build();

        final CloseableHttpClient httpClient = HttpClients.custom()
                .useSystemProperties()
                .disableAuthCaching()
                .disableAutomaticRetries()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl.toString() + "/__admin")
                .defaultHeaders(h -> {
                    h.setContentType(MediaType.APPLICATION_JSON);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }
}
