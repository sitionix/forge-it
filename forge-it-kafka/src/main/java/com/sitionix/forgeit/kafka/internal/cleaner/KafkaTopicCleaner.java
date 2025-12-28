package com.sitionix.forgeit.kafka.internal.cleaner;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
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
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public final class KafkaTopicCleaner {

    private static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaProperties kafkaProperties;

    public void reset() {
        final Map<String, Object> adminConfig = this.kafkaProperties.buildAdminProperties();
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
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to clean Kafka topics before test", ex);
        }
    }
}
