package com.sitionix.forgeit.mockmvc.internal.config;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Mock MVC feature infrastructure into the test application context.
 */
public final class MockMvcFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends MockMvcSupport> featureType() {
        return MockMvcSupport.class;
    }

    @Override
    public void install(FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("Mock MVC installer requires a BeanDefinitionRegistry context");
        }
        new AnnotatedBeanDefinitionReader(registry).register(MockMvcFeatureConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.sitionix.forgeit.mockmvc.internal")
    static class MockMvcFeatureConfiguration {
    }
}
