package com.sitionix.forgeit.kafka.internal.cleaner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DeleteRecordsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public final class KafkaTopicCleaner {

    private static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final int RESET_ATTEMPTS = 3;

    private final KafkaProperties kafkaProperties;
    private final Environment environment;

    public void reset() {
        for (int attempt = 1; attempt <= RESET_ATTEMPTS; attempt++) {
            try {
                this.resetOnce();
                return;
            } catch (final Exception ex) {
                if (attempt == RESET_ATTEMPTS) {
                    throw new IllegalStateException("Failed to clean Kafka topics before test", ex);
                }
                log.warn("Kafka topic cleanup attempt {}/{} failed; retrying.",
                        attempt,
                        RESET_ATTEMPTS,
                        ex);
                this.sleepBeforeRetry();
            }
        }
    }

    private void resetOnce() throws Exception {
        final Map<String, Object> adminConfig = this.buildAdminConfig();
        try (AdminClient adminClient = AdminClient.create(adminConfig)) {
            final Set<String> topics = adminClient.listTopics(new ListTopicsOptions().listInternal(false))
                    .names()
                    .get(ADMIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (topics.isEmpty()) {
                return;
            }
            final DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(topics);
            final Map<String, TopicDescription> descriptions = describeTopicsResult.all()
                    .get(ADMIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            final Map<TopicPartition, OffsetSpec> offsets = new HashMap<>();
            for (final TopicDescription description : descriptions.values()) {
                for (final TopicPartitionInfo partition : description.partitions()) {
                    offsets.put(new TopicPartition(description.name(), partition.partition()), OffsetSpec.latest());
                }
            }
            if (offsets.isEmpty()) {
                return;
            }
            final ListOffsetsResult offsetsResult = adminClient.listOffsets(offsets);
            final Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = offsetsResult.all()
                    .get(ADMIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            final Map<TopicPartition, RecordsToDelete> records = new HashMap<>();
            for (final Map.Entry<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> entry : endOffsets.entrySet()) {
                records.put(entry.getKey(), RecordsToDelete.beforeOffset(entry.getValue().offset()));
            }
            if (records.isEmpty()) {
                return;
            }
            final DeleteRecordsResult deleteRecordsResult = adminClient.deleteRecords(records);
            deleteRecordsResult.all().get(ADMIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private Map<String, Object> buildAdminConfig() {
        final Map<String, Object> adminConfig = new HashMap<>(this.kafkaProperties.buildAdminProperties());
        final String bootstrapServers = this.resolveBootstrapServers();
        if (bootstrapServers != null && !bootstrapServers.isBlank()) {
            adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        }
        return adminConfig;
    }

    private String resolveBootstrapServers() {
        String bootstrapServers = this.environment.getProperty("spring.kafka.bootstrap-servers");
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            bootstrapServers = this.environment.getProperty("forge-it.modules.kafka.bootstrap-servers");
        }
        return bootstrapServers;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka topic cleanup interrupted", ex);
        }
    }
}
