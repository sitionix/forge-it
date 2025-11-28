package com.sitionix.forgeit.core.autoconfigure;

import com.sitionix.forgeit.core.internal.feature.ForgeFeaturesResolver;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationService;
import com.sitionix.forgeit.core.internal.proxy.ContractProxyFactory;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.core.api.ForgeIT;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AutoConfiguration
public class ForgeItAutoConfiguration {

    @Bean
    static BeanDefinitionRegistryPostProcessor forgeItContractRegistrar() {
        return new ForgeItContractRegistrar();
    }

    private static final class ForgeItContractRegistrar implements BeanDefinitionRegistryPostProcessor {

        private final Set<Class<? extends FeatureSupport>> features = new LinkedHashSet<>();

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            if (!(registry instanceof ConfigurableApplicationContext context)) {
                return;
            }
            if (!AutoConfigurationPackages.has(context)) {
                return;
            }
            final List<String> packages = AutoConfigurationPackages.get(context);
            final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(ForgeIT.class));
            for (String basePackage : packages) {
                for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                    final Class<?> contract = ClassUtils.resolveClassName(candidate.getBeanClassName(), context.getClassLoader());
                    if (!contract.isInterface()) {
                        continue;
                    }
                    this.features.addAll(ForgeFeaturesResolver.resolveFeatures(contract));
                    registerContract(registry, context, contract);
                }
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (this.features.isEmpty()) {
                return;
            }
            if (beanFactory.containsBeanDefinition(FeatureInstallerLifecycle.BEAN_NAME)) {
                return;
            }
            final RootBeanDefinition lifecycle = new RootBeanDefinition(FeatureInstallerLifecycle.class);
            lifecycle.getConstructorArgumentValues().addIndexedArgumentValue(0, List.copyOf(this.features));
            beanFactory.registerBeanDefinition(FeatureInstallerLifecycle.BEAN_NAME, lifecycle);
        }

        private void registerContract(BeanDefinitionRegistry registry, ConfigurableApplicationContext context, Class<?> contract) {
            final String beanName = contract.getName();
            if (registry.containsBeanDefinition(beanName)) {
                return;
            }
            final RootBeanDefinition definition = new RootBeanDefinition(contract);
            definition.setInstanceSupplier(() -> ContractProxyFactory.createContractProxy(context.getClassLoader(), contract));
            registry.registerBeanDefinition(beanName, definition);
        }
    }

    private static final class FeatureInstallerLifecycle implements SmartLifecycle, ApplicationContextAware {

        static final String BEAN_NAME = FeatureInstallerLifecycle.class.getName();

        private final List<Class<? extends FeatureSupport>> features;
        private ConfigurableApplicationContext applicationContext;
        private boolean running;

        private FeatureInstallerLifecycle(List<Class<? extends FeatureSupport>> features) {
            this.features = features;
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        }

        @Override
        public void start() {
            if (this.running || this.applicationContext == null) {
                return;
            }
            final FeatureInstallationService installationService =
                    new FeatureInstallationService(this.applicationContext.getClassLoader());
            installationService.installFeatures(this.features, new FeatureInstallationContext(this.applicationContext));
            this.running = true;
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return this.running;
        }
    }
}
