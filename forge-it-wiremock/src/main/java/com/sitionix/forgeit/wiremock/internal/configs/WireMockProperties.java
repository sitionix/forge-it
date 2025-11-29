package com.sitionix.forgeit.wiremock.internal.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for WireMock settings exposed via {@code forge-it.modules.wiremock}.
 */
@Data
@ConfigurationProperties(prefix = WireMockProperties.PROPERTY_PREFIX)
@Component(WireMockProperties.BEAN_NAME)
public final class WireMockProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.wiremock";
    static final String BEAN_NAME = "forgeItWireMockProperties";

    private Boolean enabled;
    private String host;
    private Integer port;
    private Mapping mapping;
    private Mode mode;

    @Data
    public static class Mapping {
        private String request;
        private String response;
        private String defaultRequest;
        private String defaultResponse;
    }

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }
}
