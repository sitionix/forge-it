package com.sitionix.forgeit.core.internal.test;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public final class ForgeItTestInitializer implements SmartInitializingSingleton, ApplicationContextAware {

    private final Class<?> contractType;
    private final List<Class<?>> features;
    private ConfigurableApplicationContext context;

    public ForgeItTestInitializer(Class<?> contractType, List<Class<?>> features) {
        this.contractType = contractType;
        this.features = List.copyOf(features);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
            this.context = configurableContext;
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (this.context == null) {
            return;
        }
        try {
            final ClassLoader classLoader = this.context.getClassLoader();
            final Class<?> installationServiceType = classLoader.loadClass(
                    "com.sitionix.forgeit.core.internal.feature.FeatureInstallationService");
            final Constructor<?> serviceConstructor = installationServiceType.getConstructor(ClassLoader.class);
            final Object installationService = serviceConstructor.newInstance(classLoader);

            final Class<?> installationContextType = classLoader.loadClass(
                    "com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext");
            final Constructor<?> contextConstructor =
                    installationContextType.getConstructor(ConfigurableApplicationContext.class);
            final Object installationContext = contextConstructor.newInstance(this.context);

            final Method installFeatures = installationServiceType.getMethod(
                    "installFeatures", Collection.class, installationContextType);
            installFeatures.invoke(installationService, this.features, installationContext);

            final Class<?> contractProxyFactory = classLoader.loadClass(
                    "com.sitionix.forgeit.core.internal.proxy.ContractProxyFactory");
            final Method registerProxy = contractProxyFactory.getMethod(
                    "registerContractProxy", ConfigurableApplicationContext.class, Class.class);
            registerProxy.invoke(null, this.context, this.contractType);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize ForgeIT contract proxy", ex);
        }
    }
}
