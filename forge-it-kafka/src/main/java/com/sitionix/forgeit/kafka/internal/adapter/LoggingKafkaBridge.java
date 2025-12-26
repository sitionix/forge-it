package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaBridge;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logging adapter that confirms Kafka bridge availability.
 */
@Component
public final class LoggingKafkaBridge implements KafkaBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingKafkaBridge.class);

    private final KafkaProperties properties;

    public LoggingKafkaBridge(final KafkaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void bridge() {
        LOGGER.info("Kafka bridge active (mode={}, bootstrapServers={})",
                this.properties.getMode(),
                this.properties.getBootstrapServers());
    }
}
