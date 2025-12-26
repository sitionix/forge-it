package com.sitionix.forgeit.consumer.kafka.producer;

import com.sitionix.forgeit.consumer.kafka.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "consumer.kafka", name = "enabled", havingValue = "true")
public class ForgeItKafkaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeItKafkaProducer.class);

    private final KafkaTopicConfig config;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ForgeItKafkaProducer(final KafkaTopicConfig config, final KafkaTemplate<String, String> kafkaTemplate) {
        this.config = config;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(final String message) {
        LOGGER.info("Kafka producer sending message to {}: {}", this.config.getTopic(), message);
        this.kafkaTemplate.send(this.config.getTopic(), message)
                .whenComplete((SendResult<String, String> result, Throwable exception) -> {
                    if (exception != null) {
                        LOGGER.error("Kafka producer failed to send message.", exception);
                        return;
                    }
                    LOGGER.info("Kafka producer stored message at {}-{} offset {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
