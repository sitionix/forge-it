package com.sitionix.forgeit.ros.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("forge-it.modules.ros")
public class RosProperties {

    private boolean enabled = true;
    private String pythonCommand = "python3";
    private Duration startupTimeout = Duration.ofSeconds(10);
    private Duration shutdownTimeout = Duration.ofSeconds(10);
    private Duration defaultConsumeTimeout = Duration.ofSeconds(5);
    private String domainId = "";

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean value) {
        this.enabled = value;
    }

    public String getPythonCommand() {
        return this.pythonCommand;
    }

    public void setPythonCommand(final String value) {
        this.pythonCommand = value;
    }

    public Duration getStartupTimeout() {
        return this.startupTimeout;
    }

    public void setStartupTimeout(final Duration value) {
        this.startupTimeout = value;
    }

    public Duration getShutdownTimeout() {
        return this.shutdownTimeout;
    }

    public void setShutdownTimeout(final Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("forge-it.modules.ros.shutdown-timeout must be positive");
        }
        this.shutdownTimeout = value;
    }

    public Duration getDefaultConsumeTimeout() {
        return this.defaultConsumeTimeout;
    }

    public void setDefaultConsumeTimeout(final Duration value) {
        this.defaultConsumeTimeout = value;
    }

    public String getDomainId() {
        return this.domainId;
    }

    public void setDomainId(final String value) {
        this.domainId = value;
    }
}
