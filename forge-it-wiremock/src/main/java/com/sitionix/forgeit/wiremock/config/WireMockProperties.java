package com.sitionix.forgeit.wiremock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * Spring Boot view of the WireMock module configuration.
 */
@Component
@ConfigurationProperties(prefix = "forge-it.modules.wiremock")
public class WireMockProperties {

    /**
     * Whether WireMock support is enabled at all.
     */
    private boolean enabled = true;

    /**
     * Whether WireMock should be started internally or expected to run externally.
     */
    private Mode mode = Mode.INTERNAL;

    /**
     * Hostname WireMock is reachable on.
     */
    private String host = "localhost";

    /**
     * Port WireMock listens on.
     */
    private int port = 8089;

    @NestedConfigurationProperty
    private final Record record = new Record();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Record getRecord() {
        return record;
    }

    /**
     * Computed base URL constructed from the current host/port.
     */
    public String getBaseUrl() {
        return String.format("http://%s:%d", host, port);
    }

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }

    public static class Record {

        /**
         * Whether HTTP traffic recording is enabled.
         */
        private boolean enabled;

        /**
         * Target base URL used when recording interactions.
         */
        private String targetBaseUrl;

        /**
         * Whether recorded stubs should be persisted to disk.
         */
        private boolean persistMappings = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTargetBaseUrl() {
            return targetBaseUrl;
        }

        public void setTargetBaseUrl(String targetBaseUrl) {
            this.targetBaseUrl = targetBaseUrl;
        }

        public boolean isPersistMappings() {
            return persistMappings;
        }

        public void setPersistMappings(boolean persistMappings) {
            this.persistMappings = persistMappings;
        }
    }
}
