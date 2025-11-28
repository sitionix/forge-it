package com.sitionix.forgeit.wiremock.internal.configs;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import static com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal.BEAN_NAME;

/**
 * Registers the WireMock infrastructure components when the feature is
 * requested by a ForgeIT integration test contract.
 */
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
        registerProperties(context, registry);
        registerContainerManager(context, registry);
        registerJournal(registry);
        registerFacade(registry);
    }

    private void registerProperties(FeatureInstallationContext context, BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(WireMockProperties.BEAN_NAME)) {
            return;
        }
        final BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(WireMockProperties.class)
                .getBeanDefinition();
        registry.registerBeanDefinition(WireMockProperties.BEAN_NAME, beanDefinition);
    }

    private void registerContainerManager(FeatureInstallationContext context, BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(WireMockContainerManager.BEAN_NAME)) {
            return;
        }
        final BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(WireMockContainerManager.class)
                .addConstructorArgValue(context.environment())
                .addConstructorArgReference(WireMockProperties.BEAN_NAME)
                .getBeanDefinition();
        registry.registerBeanDefinition(WireMockContainerManager.BEAN_NAME, beanDefinition);
    }

    private void registerJournal(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(BEAN_NAME)) {
            return;
        }
        final BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(WireMockJournal.class)
                .getBeanDefinition();
        registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
    }

    private void registerFacade(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(WireMockFacade.BEAN_NAME)) {
            return;
        }
        final BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(WireMockFacade.class)
                .addConstructorArgReference(BEAN_NAME)
                .getBeanDefinition();
        registry.registerBeanDefinition(WireMockFacade.BEAN_NAME, beanDefinition);
    }
}
