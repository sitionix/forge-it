package com.sitionix.forgeit.kafka.internal.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
final class KafkaPropertiesDependsOnContainer implements BeanFactoryPostProcessor, PriorityOrdered {

    private static final String KAFKA_CONTAINER_MANAGER = "kafkaContainerManager";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
        final String[] kafkaPropertiesBeans = beanFactory.getBeanNamesForType(KafkaProperties.class, false, false);
        for (final String beanName : kafkaPropertiesBeans) {
            if (!beanFactory.containsBeanDefinition(beanName)) {
                continue;
            }
            final BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
            definition.setDependsOn(mergeDependsOn(definition.getDependsOn()));
        }
    }

    private String[] mergeDependsOn(final String[] existingDependsOn) {
        final Set<String> dependsOn = new LinkedHashSet<>();
        if (existingDependsOn != null) {
            dependsOn.addAll(Arrays.asList(existingDependsOn));
        }
        dependsOn.add(KAFKA_CONTAINER_MANAGER);
        return dependsOn.toArray(new String[0]);
    }
}
