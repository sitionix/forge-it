package com.sitionix.forgeit.mockmvc.internal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.core.internal.test.TestExecutionMode;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import com.sitionix.forgeit.mockmvc.config.MockMvcDefaultsEnvironmentPostProcessor;
import com.sitionix.forgeit.mockmvc.internal.executor.HttpExecutor;
import com.sitionix.forgeit.mockmvc.internal.journal.MockMvcJournal;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import java.net.http.HttpClient;

/** Registers only the infrastructure for the context's selected transport. */
public final class MockMvcFeatureInstaller implements FeatureInstaller {
    @Override public Class<? extends MockMvcSupport> featureType() { return MockMvcSupport.class; }

    @Override public void install(FeatureInstallationContext context) {
        if (!(context.applicationContext() instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("Mock MVC installer requires a BeanDefinitionRegistry context");
        }
        final boolean e2e = context.beanFactory().getSingleton(TestExecutionMode.class.getName()) == TestExecutionMode.E2E;
        if (e2e) {
            new MockMvcDefaultsEnvironmentPostProcessor().postProcessEnvironment(context.environment(), null);
        }
        new AnnotatedBeanDefinitionReader(registry).register(MockMvcLoader.class,
                e2e ? HttpConfiguration.class : MvcConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MockMvcProperties.class)
    static class MvcConfiguration {
        @Bean MockMvcJournal mockMvcJournal(ObjectMapper mapper, MockMvcLoader loader,
                                            MockMvc mvc, MockMvcProperties properties) {
            return new MockMvcJournal(mapper, loader, mvc, properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MockMvcProperties.class)
    static class HttpConfiguration {
        @Bean(destroyMethod = "shutdownNow") HttpClient forgeItHttpClient(MockMvcProperties properties) {
            HttpExecutor.requireTimeout(properties.getRequestTimeout(), "request-timeout");
            return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(HttpExecutor.requireTimeout(properties.getConnectTimeout(), "connect-timeout"))
                    .build();
        }
        @Bean MockMvcJournal mockMvcJournal(ObjectMapper mapper, MockMvcLoader loader,
                                            MockMvcProperties properties, Environment environment, HttpClient client) {
            return MockMvcJournal.forHttp(mapper, loader, properties, environment, client);
        }
    }
}
