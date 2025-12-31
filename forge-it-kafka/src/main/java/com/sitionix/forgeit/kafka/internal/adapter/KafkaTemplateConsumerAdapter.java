package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@DependsOn("kafkaContainerManager")
@RequiredArgsConstructor
public class KafkaTemplateConsumerAdapter implements KafkaConsumerPort, DisposableBean {

    private static final long DEFAULT_POLL_TIMEOUT_MS = 5000L;

    @SuppressWarnings("rawtypes")
    private final ObjectProvider<ConsumerFactory> consumerFactoryProvider;
    private final KafkaProperties properties;
    private final Environment environment;
    private final Object consumerLock = new Object();
    private Consumer<?, ?> consumer;
    private String consumerGroupId;

    @Override
    public <T> Object consume(final KafkaContract<T> contract, final Duration timeout) {
        final String topic = this.environment.resolveRequiredPlaceholders(contract.getTopic());
        final Object payload = this.consumeIfPresent(contract, timeout);
        if (payload == null) {
            throw new IllegalStateException("Kafka message was not received within timeout for topic=" +
                    topic);
        }
        return payload;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> Object consumeIfPresent(final KafkaContract<T> contract, final Duration timeout) {
        final ConsumerFactory consumerFactory = this.consumerFactoryProvider.getIfAvailable();
        if (consumerFactory == null) {
            throw new IllegalStateException("ConsumerFactory bean is not available; ensure spring-kafka is configured");
        }
        final KafkaProperties.Consumer consumerConfig = this.properties != null ? this.properties.getConsumer() : null;
        final String groupId = this.resolveGroupId(contract, consumerConfig);
        final Duration effectiveTimeout = Optional.ofNullable(timeout)
                .orElse(Duration.ofMillis(this.resolvePollTimeoutMs(consumerConfig)));
        final String topic = this.environment.resolveRequiredPlaceholders(contract.getTopic());

        final Consumer<?, ?> consumer = this.resolveConsumer(consumerFactory, groupId);
        consumer.unsubscribe();
        consumer.subscribe(List.of(topic));
        final long deadline = System.nanoTime() + effectiveTimeout.toNanos();
        while (System.nanoTime() < deadline) {
            final Duration pollDuration = Duration.ofMillis(Math.max(1L,
                    (deadline - System.nanoTime()) / 1_000_000L));
            final ConsumerRecords<?, ?> records = consumer.poll(pollDuration);
            for (final ConsumerRecord<?, ?> record : records) {
                return record.value();
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        synchronized (this.consumerLock) {
            this.closeConsumer();
        }
    }

    @SuppressWarnings("rawtypes")
    private Consumer<?, ?> resolveConsumer(final ConsumerFactory consumerFactory, final String groupId) {
        synchronized (this.consumerLock) {
            if (this.consumer == null || !groupId.equals(this.consumerGroupId)) {
                this.closeConsumer();
                this.consumer = consumerFactory.createConsumer(groupId);
                this.consumerGroupId = groupId;
            }
            return this.consumer;
        }
    }

    private void closeConsumer() {
        if (this.consumer != null) {
            this.consumer.wakeup();
            this.consumer.close(Duration.ofSeconds(1));
            this.consumer = null;
            this.consumerGroupId = null;
        }
    }

    private long resolvePollTimeoutMs(final KafkaProperties.Consumer consumerConfig) {
        if (consumerConfig == null) {
            return DEFAULT_POLL_TIMEOUT_MS;
        }
        final long pollTimeoutMs = consumerConfig.getPollTimeoutMs();
        return pollTimeoutMs > 0 ? pollTimeoutMs : DEFAULT_POLL_TIMEOUT_MS;
    }

    private String resolveGroupId(final KafkaContract<?> contract, final KafkaProperties.Consumer consumerConfig) {
        final String contractGroupId = contract.getConsumerGroupId();
        final String groupId = (contractGroupId != null && !contractGroupId.isBlank())
                ? contractGroupId
                : consumerConfig != null ? consumerConfig.getGroupId() : null;
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalStateException("Kafka consumer groupId must be provided via contract or "
                    + "forge-it.modules.kafka.consumer.group-id");
        }
        return this.environment.resolveRequiredPlaceholders(groupId);
    }
}
