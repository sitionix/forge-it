package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTemplatePublisherAdapter implements KafkaPublisherPort {

    @SuppressWarnings("rawtypes")
    private final ObjectProvider<KafkaTemplate> kafkaTemplateProvider;
    private final Environment environment;

    @Override
    @SuppressWarnings("rawtypes")
    public <T> void publish(final KafkaContract<T> contract, final Object payload, final String key) {
        final KafkaTemplate kafkaTemplate = this.kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            throw new IllegalStateException("KafkaTemplate bean is not available; ensure spring-kafka is configured");
        }
        final String topic = this.environment.resolveRequiredPlaceholders(contract.getTopic());
        final ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        kafkaTemplate.send(record);
    }
}
