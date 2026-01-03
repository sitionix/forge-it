package com.sitionix.forgeit.kafka.internal.adapter;

import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaTemplatePublisherAdapter implements KafkaPublisherPort {

    @SuppressWarnings("rawtypes")
    private final ObjectProvider<KafkaTemplate> kafkaTemplateProvider;
    private final KafkaProperties kafkaProperties;
    private final Environment environment;
    private final Object byteArrayTemplateLock = new Object();
    private volatile KafkaTemplate<String, byte[]> byteArrayKafkaTemplate;

    @Override
    @SuppressWarnings("rawtypes")
    public <T> void publish(final KafkaContract<T> contract, final Object payload, final String key) {
        final String topic = this.environment.resolveRequiredPlaceholders(contract.getTopic());
        if (payload instanceof byte[]) {
            final KafkaTemplate<String, byte[]> kafkaTemplate = this.resolveByteArrayTemplate();
            final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, (byte[]) payload);
            kafkaTemplate.send(record);
            return;
        }
        final KafkaTemplate kafkaTemplate = this.kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            throw new IllegalStateException("KafkaTemplate bean is not available; ensure spring-kafka is configured");
        }
        final ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        kafkaTemplate.send(record);
    }

    private KafkaTemplate<String, byte[]> resolveByteArrayTemplate() {
        KafkaTemplate<String, byte[]> template = this.byteArrayKafkaTemplate;
        if (template != null) {
            return template;
        }
        synchronized (this.byteArrayTemplateLock) {
            template = this.byteArrayKafkaTemplate;
            if (template != null) {
                return template;
            }
            final Map<String, Object> props = new LinkedHashMap<>(this.kafkaProperties.buildProducerProperties());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
            final ProducerFactory<String, byte[]> factory = new DefaultKafkaProducerFactory<>(props);
            template = new KafkaTemplate<>(factory);
            this.byteArrayKafkaTemplate = template;
            return template;
        }
    }
}
