package com.sitionix.forgeit.ros.internal.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("forge-it.modules.ros")
public class RosProperties {
    public static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);
    private boolean enabled = true;
    private String pythonCommand = "python3";
    private Duration startupTimeout = Duration.ofSeconds(10);
    private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
    private Duration defaultConsumeTimeout = Duration.ofSeconds(5);
    private String domainId = "";
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getPythonCommand() { return pythonCommand; }
    public void setPythonCommand(String value) { pythonCommand = value; }
    public Duration getStartupTimeout() { return startupTimeout; }
    public void setStartupTimeout(Duration value) { startupTimeout = value; }
    public Duration getShutdownTimeout() { return shutdownTimeout; }
    public void setShutdownTimeout(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("forge-it.modules.ros.shutdown-timeout must be positive");
        }
        shutdownTimeout = value;
    }
    public Duration getDefaultConsumeTimeout() { return defaultConsumeTimeout; }
    public void setDefaultConsumeTimeout(Duration value) { defaultConsumeTimeout = value; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String value) { domainId = value; }
}
