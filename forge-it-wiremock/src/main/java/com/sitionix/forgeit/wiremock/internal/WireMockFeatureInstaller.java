package com.sitionix.forgeit.wiremock.internal;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public final class WireMockFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends FeatureSupport> featureType() {
        return WireMockSupport.class;
    }

    @Override
    public void install(FeatureInstallationContext context) {
        if (!(context.beanFactory() instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("ForgeIT requires a BeanDefinitionRegistry-backed context");
        }
        if (!registry.containsBeanDefinition(WireMockFacade.BEAN_NAME)) {
            final BeanDefinition beanDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(WireMockFacade.class)
                    .getBeanDefinition();
            registry.registerBeanDefinition(WireMockFacade.BEAN_NAME, beanDefinition);
        }
    }
}
