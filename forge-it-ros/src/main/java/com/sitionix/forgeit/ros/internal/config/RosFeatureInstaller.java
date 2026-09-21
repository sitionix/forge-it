package com.sitionix.forgeit.ros.internal.config;

import com.sitionix.forgeit.application.loader.file.FileLoader;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.ros.api.RosMessaging;
import com.sitionix.forgeit.ros.api.RosSupport;
import com.sitionix.forgeit.ros.internal.service.RosMessagingFacade;
import com.sitionix.forgeit.ros.internal.transport.PythonRosTransport;
import com.sitionix.forgeit.ros.internal.transport.RosTransport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** ROS is installed only when explicitly selected, in IT or E2E. */
public final class RosFeatureInstaller implements FeatureInstaller {
    @Override public Class<RosSupport> featureType() { return RosSupport.class; }
    @Override public void install(FeatureInstallationContext context) {
        if (!(context.applicationContext() instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("ROS installer requires a BeanDefinitionRegistry context");
        }
        new com.sitionix.forgeit.ros.config.RosDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(context.environment(), null);
        new AnnotatedBeanDefinitionReader(registry).register(RosConfiguration.class);
    }
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RosProperties.class)
    static class RosConfiguration {
        @Bean(destroyMethod = "close") RosTransport rosTransport(RosProperties properties) {
            if (!properties.isEnabled()) throw new IllegalStateException("ROS feature selected but disabled by configuration");
            return new PythonRosTransport(properties.getPythonCommand(), properties.getStartupTimeout(), properties.getShutdownTimeout(), properties.getDomainId());
        }
        @Bean RosMessaging rosMessaging(RosTransport transport, Environment environment, RosProperties properties) {
            return new RosMessagingFacade(transport, environment, properties, FileLoader::load);
        }
    }
}
