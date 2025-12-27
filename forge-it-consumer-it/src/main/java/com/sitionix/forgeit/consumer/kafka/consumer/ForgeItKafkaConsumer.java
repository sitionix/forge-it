package com.sitionix.forgeit.consumer.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.consumer.kafka.KafkaTopicConfig;
import com.sitionix.forgeit.consumer.kafka.domain.UserCreatedEvent;
import com.sitionix.forgeit.consumer.kafka.domain.UserEnvelope;
import com.sitionix.forgeit.consumer.kafka.producer.ForgeItKafkaProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@ConditionalOnProperty(prefix = "consumer.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ForgeItKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeItKafkaConsumer.class);

    private final KafkaTopicConfig config;
    private final ObjectMapper objectMapper;
    private final ForgeItKafkaProducer producer;
    private final BlockingQueue<UserEnvelope> messages = new LinkedBlockingQueue<>();

    @KafkaListener(topics = "#{@kafkaTopicConfig.inputTopic}")
    public void handleMessage(final String message) {
        try {
            final UserEnvelope envelope = this.objectMapper.readValue(message, UserEnvelope.class);
            this.messages.add(envelope);
            LOGGER.info("Kafka consumer received message from {}: {}", this.config.getInputTopic(), message);
            this.producer.sendUserCreated(envelope);
        } catch (final Exception ex) {
            LOGGER.error("Kafka consumer failed to parse message from {}", this.config.getInputTopic(), ex);
        }
    }

    @KafkaListener(topics = "#{@kafkaTopicConfig.payloadInputTopic}")
    public void handlePayloadMessage(final String message) {
        try {
            final UserCreatedEvent event = this.objectMapper.readValue(message, UserCreatedEvent.class);
            LOGGER.info("Kafka consumer received payload message from {}: {}", this.config.getPayloadInputTopic(),
                    message);
            this.producer.sendUserCreatedPayload(event);
        } catch (final Exception ex) {
            LOGGER.error("Kafka consumer failed to parse payload message from {}", this.config.getPayloadInputTopic(),
                    ex);
        }
    }

}
