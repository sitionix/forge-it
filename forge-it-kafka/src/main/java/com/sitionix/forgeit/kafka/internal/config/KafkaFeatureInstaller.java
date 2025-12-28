package com.sitionix.forgeit.kafka.internal.config;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.kafka.api.KafkaSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Kafka feature infrastructure into the test application context.
 */
public final class KafkaFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends KafkaSupport> featureType() {
        return KafkaSupport.class;
    }

    @Override
    public void install(final FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof final BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("Kafka installer requires a BeanDefinitionRegistry context");
        }
        registry.registerBeanDefinition(KafkaFeatureMarker.class.getName(),
                new RootBeanDefinition(KafkaFeatureMarker.class));
        new AnnotatedBeanDefinitionReader(registry).register(KafkaFeatureConfiguration.class);
    }

    static final class KafkaFeatureMarker {
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.sitionix.forgeit.kafka.internal")
    static class KafkaFeatureConfiguration {
    }
}
