package com.sitionix.forgeit.ros.internal.config;

import com.sitionix.forgeit.application.loader.file.FileLoader;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.ros.api.RosMessaging;
import com.sitionix.forgeit.ros.api.RosSupport;
import com.sitionix.forgeit.ros.config.RosDefaultsEnvironmentPostProcessor;
import com.sitionix.forgeit.ros.internal.adapter.PythonRosTransport;
import com.sitionix.forgeit.ros.internal.loader.RosLoader;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import com.sitionix.forgeit.ros.internal.service.RosMessagingFacade;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

public final class RosFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<RosSupport> featureType() {
        return RosSupport.class;
    }

    @Override
    public void install(final FeatureInstallationContext context) {
        if (!(context.applicationContext() instanceof final BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("ROS installer requires a BeanDefinitionRegistry context");
        }
        new RosDefaultsEnvironmentPostProcessor().postProcessEnvironment(context.environment(), null);
        new AnnotatedBeanDefinitionReader(registry).register(RosConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RosProperties.class)
    static class RosConfiguration {

        @Bean(destroyMethod = "close")
        PythonRosTransport rosTransport(final RosProperties properties) {
            if (!properties.isEnabled()) {
                throw new IllegalStateException("ROS feature selected but disabled by configuration");
            }
            return new PythonRosTransport(properties.getPythonCommand(), properties.getStartupTimeout(),
                    properties.getShutdownTimeout(), properties.getDomainId());
        }

        @Bean
        RosLoader rosLoader() {
            return new RosLoader(FileLoader::load);
        }

        @Bean
        RosMessaging rosMessaging(final RosLoader rosLoader, final Environment environment,
                                  final RosProperties properties, final RosPublisherPort publisherPort,
                                  final RosConsumerPort consumerPort) {
            return new RosMessagingFacade(rosLoader, environment, properties, publisherPort, consumerPort);
        }
    }
}
