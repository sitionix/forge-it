package com.sitionix.forgeit.mockmvc.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.time.Duration;


/**
 * Configuration model for WireMock settings exposed via {@code forge-it.modules.mock-mvc}.
 */
@Data
@ConfigurationProperties(prefix = MockMvcProperties.PROPERTY_PREFIX)
@Component
public class MockMvcProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.mock-mvc";

    private Boolean enabled;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration requestTimeout = Duration.ofSeconds(10);
    private Path path;
    private String defaultToken;
    private Map<String, String> defaultHeaders;
    @Data
    public static class Path {
        private String request;
        private String response;
        private String defaultRequest;
        private String defaultResponse;
    }
}
