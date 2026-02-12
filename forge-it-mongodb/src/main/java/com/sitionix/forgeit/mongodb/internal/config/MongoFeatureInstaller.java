package com.sitionix.forgeit.mongodb.internal.config;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.mongodb.api.MongoSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the MongoDB feature infrastructure into the test application context.
 */
public final class MongoFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends MongoSupport> featureType() {
        return MongoSupport.class;
    }

    @Override
    public void install(final FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof final BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("Mongo installer requires a BeanDefinitionRegistry context");
        }
        new AnnotatedBeanDefinitionReader(registry).register(MongoFeatureConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.sitionix.forgeit.mongodb.internal")
    static class MongoFeatureConfiguration {
    }
}
