package com.sitionix.forgeit.consumer.kafka.consumer;

import com.sitionix.forgeit.consumer.kafka.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "consumer.kafka", name = "enabled", havingValue = "true")
public class ForgeItKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeItKafkaConsumer.class);

    private final KafkaTopicConfig config;

    public ForgeItKafkaConsumer(final KafkaTopicConfig config) {
        this.config = config;
    }

    @KafkaListener(topics = "#{@kafkaTopicConfig.topic}")
    public void handleMessage(final String message) {
        LOGGER.info("Kafka consumer received message from {}: {}", this.config.getTopic(), message);
    }
}
