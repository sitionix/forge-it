package com.sitionix.forgeit.kafka.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for Kafka settings exposed via {@code forge-it.modules.kafka}.
 */
@Data
@ConfigurationProperties(prefix = KafkaProperties.PROPERTY_PREFIX)
@Component
public final class KafkaProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.kafka";

    private Boolean enabled;
    private Mode mode;
    private String bootstrapServers;

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }
}
