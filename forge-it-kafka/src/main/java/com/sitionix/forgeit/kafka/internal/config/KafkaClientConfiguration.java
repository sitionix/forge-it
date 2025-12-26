package com.sitionix.forgeit.kafka.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration(proxyBeanMethods = false)
class KafkaClientConfiguration {

    @Bean
    @DependsOn("kafkaContainerManager")
    @ConditionalOnMissingBean(ProducerFactory.class)
    ProducerFactory<String, String> kafkaProducerFactory(final KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    @DependsOn("kafkaContainerManager")
    @ConditionalOnMissingBean(ConsumerFactory.class)
    ConsumerFactory<String, String> kafkaConsumerFactory(final KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    @DependsOn("kafkaContainerManager")
    @ConditionalOnMissingBean(KafkaTemplate.class)
    KafkaTemplate<String, String> kafkaTemplate(final ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
