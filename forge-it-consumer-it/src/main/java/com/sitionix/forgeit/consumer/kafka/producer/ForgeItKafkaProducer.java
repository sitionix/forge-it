package com.sitionix.forgeit.consumer.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.consumer.kafka.KafkaTopicConfig;
import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import org.apache.kafka.clients.producer.ProducerRecord;

@Service
@ConditionalOnProperty(prefix = "consumer.kafka", name = "enabled", havingValue = "true")
public class ForgeItKafkaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeItKafkaProducer.class);

    private final KafkaTopicConfig config;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ForgeItKafkaProducer(final KafkaTopicConfig config,
                                final KafkaTemplate<String, String> kafkaTemplate,
                                final ObjectMapper objectMapper) {
        this.config = config;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendUserCreated(final UserCreatedEvent event) {
        final String payload = this.writeValueAsString(event);
        LOGGER.info("Kafka producer sending message to {}: {}", this.config.getOutputTopic(), payload);
        final ProducerRecord<String, String> record = new ProducerRecord<>(this.config.getOutputTopic(),
                event.getUserId(),
                payload);
        this.kafkaTemplate.send(record)
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

    private String writeValueAsString(final UserCreatedEvent event) {
        try {
            return this.objectMapper.writeValueAsString(event);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka message", ex);
        }
    }
}
