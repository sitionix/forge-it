package com.sitionix.forgeit.core.internal.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.application.loader.json.JsonLoaderImpl;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationService;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.ClassUtils;
import java.util.List;

/** Plain Spring context: config data and selected ForgeIT infrastructure, no application autoconfiguration. */
record ForgeE2eContextCustomizer(Class<?> contractType, List<Class<? extends FeatureSupport>> features,
                                 List<String> properties) implements ContextCustomizer {
    @Override public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration merged) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, properties.toArray(String[]::new));
        new ConfigDataApplicationContextInitializer().initialize(context);
        final BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context;
        context.getBeanFactory().registerSingleton(TestExecutionMode.class.getName(), TestExecutionMode.E2E);
        registry.registerBeanDefinition("objectMapper", new RootBeanDefinition(ObjectMapper.class,
                () -> new ObjectMapper().findAndRegisterModules()));
        registry.registerBeanDefinition("jsonLoader", new RootBeanDefinition(JsonLoaderImpl.class));
        final String implementationName = contractType.getPackageName() + "." + contractType.getSimpleName() + "Impl";
        final Class<?> implementation = ClassUtils.resolveClassName(implementationName, context.getClassLoader());
        if (!contractType.isAssignableFrom(implementation)) {
            throw new IllegalStateException("Invalid generated ForgeIT implementation: " + implementationName);
        }
        registry.registerBeanDefinition("forgeItConsumer", new RootBeanDefinition(implementation));
        new FeatureInstallationService(context.getClassLoader()).installFeatures(features, new FeatureInstallationContext(context));
    }
}
