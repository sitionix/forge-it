package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KafkaTemplateConsumerAdapter implements KafkaConsumerPort {

    private final ObjectProvider<ConsumerFactory<String, String>> consumerFactoryProvider;
    private final KafkaProperties properties;
    private final Environment environment;

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
        final String topic = this.environment.resolveRequiredPlaceholders(contract.getTopic());

        try (final Consumer<String, String> consumer = consumerFactory.createConsumer(groupId)) {
            consumer.subscribe(java.util.List.of(topic));
            final long deadline = System.nanoTime() + effectiveTimeout.toNanos();
            while (System.nanoTime() < deadline) {
                final Duration pollDuration = Duration.ofMillis(Math.max(1L,
                        (deadline - System.nanoTime()) / 1_000_000L));
                final ConsumerRecords<String, String> records = consumer.poll(pollDuration);
                for (final ConsumerRecord<String, String> record : records) {
                    return record.value();
                }
            }
        }
        throw new IllegalStateException("Kafka message was not received within timeout for topic=" +
                topic);
    }
}
