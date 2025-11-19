package com.sitionix.forgeit.wiremock.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Configuration model for WireMock settings exposed via {@code forge-it.modules.wiremock}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = WireMockProperties.PROPERTY_PREFIX)
public final class WireMockProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.wiremock";
    static final String BEAN_NAME = "forgeItWireMockProperties";

    private Boolean enabled;
    private Mode mode;
    private String host;
    private Integer port;
    private Record record;

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }

    @Getter
    @Setter
    public static final class Record {
        private Boolean enabled;
        private URI targetBaseUrl;
        private Boolean persistMappings;
    }
}
