package com.sitionix.forgeit.wiremock.internal.configs;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

/**
 * Registers the WireMock feature infrastructure into the test application context.
 */
public final class WireMockFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends WireMockSupport> featureType() {
        return WireMockSupport.class;
    }

    @Override
    public void install(FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("WireMock installer requires a BeanDefinitionRegistry context");
        }
        new AnnotatedBeanDefinitionReader(registry).register(WireMockFeatureConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.sitionix.forgeit.wiremock.internal")
    @Import(WireMockAdminConfig.class)
    static class WireMockFeatureConfiguration {
    }
}
