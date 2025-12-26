package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class KafkaTemplatePublisherAdapter implements KafkaPublisherPort {

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Override
    public <T> void publish(final KafkaContract<T> contract, final String payloadJson, final String key) {
        final KafkaTemplate<String, String> kafkaTemplate = this.kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            throw new IllegalStateException("KafkaTemplate bean is not available; ensure spring-kafka is configured");
        }
        final ProducerRecord<String, String> record = new ProducerRecord<>(contract.getTopic(), key, payloadJson);
        record.headers().add(KafkaHeaders.EVENT_TYPE, contract.getEventType().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }
}
