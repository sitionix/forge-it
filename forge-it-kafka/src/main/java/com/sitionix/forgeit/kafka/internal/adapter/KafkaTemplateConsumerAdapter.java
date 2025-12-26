package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KafkaTemplateConsumerAdapter implements KafkaConsumerPort {

    private final ObjectProvider<ConsumerFactory<String, String>> consumerFactoryProvider;
    private final KafkaProperties properties;

    @Override
    public <T> String consume(final KafkaContract<T> contract, final Duration timeout) {
        final ConsumerFactory<String, String> consumerFactory = this.consumerFactoryProvider.getIfAvailable();
        if (consumerFactory == null) {
            throw new IllegalStateException("ConsumerFactory bean is not available; ensure spring-kafka is configured");
        }
        if (this.properties.getConsumer() == null) {
            throw new IllegalStateException("Kafka consumer properties are not configured");
        }
        final String groupId = this.properties.getConsumer().getGroupId();
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalStateException("Kafka consumer groupId is not configured");
        }
        final Duration effectiveTimeout = Optional.ofNullable(timeout)
                .orElse(Duration.ofMillis(this.properties.getConsumer().getPollTimeoutMs()));

        try (final Consumer<String, String> consumer = consumerFactory.createConsumer(groupId)) {
            consumer.subscribe(java.util.List.of(contract.getTopic()));
            final long deadline = System.nanoTime() + effectiveTimeout.toNanos();
            while (System.nanoTime() < deadline) {
                final Duration pollDuration = Duration.ofMillis(Math.max(1L,
                        (deadline - System.nanoTime()) / 1_000_000L));
                final ConsumerRecords<String, String> records = consumer.poll(pollDuration);
                for (final ConsumerRecord<String, String> record : records) {
                    if (this.matchesEventType(contract, record)) {
                        return record.value();
                    }
                }
            }
        }
        throw new IllegalStateException("Kafka message was not received within timeout for eventType=" +
                contract.getEventType());
    }

    private <T> boolean matchesEventType(final KafkaContract<T> contract,
                                         final ConsumerRecord<String, String> record) {
        final Header header = record.headers().lastHeader(KafkaHeaders.EVENT_TYPE);
        if (header == null) {
            return false;
        }
        final String headerValue = new String(header.value(), StandardCharsets.UTF_8);
        return contract.getEventType().equals(headerValue);
    }
}
