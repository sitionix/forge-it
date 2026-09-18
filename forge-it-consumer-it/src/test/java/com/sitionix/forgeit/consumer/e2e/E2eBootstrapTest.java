package com.sitionix.forgeit.consumer.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.application.loader.json.JsonLoaderImpl;
import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.internal.test.IntegrationTestContextCustomizerFactory;
import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.mockmvc.internal.journal.MockMvcJournal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class E2eBootstrapTest {
    @Test void rejectsMixedModeAndUnsupportedFeaturesBeforeContextCreation() {
        var factory = new IntegrationTestContextCustomizerFactory();
        assertThatThrownBy(() -> factory.createContextCustomizer(Mixed.class, List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("cannot be combined");
        assertThatThrownBy(() -> factory.createContextCustomizer(Unsupported.class, List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Unsupported E2E feature");
    }

    @Test void sequentialContextsKeepTheirModeConfigurationAndCachedContext() throws Exception {
        final String previous = System.getProperty("consumer.http.auth-base-url");
        System.setProperty("consumer.http.auth-base-url", "http://system.test");
        try {
            var first = new TestContextManager(First.class);
            var second = new TestContextManager(Second.class);
            var integration = new TestContextManager(MvcTest.class);
            first.beforeTestClass();
            var firstContext = first.getTestContext().getApplicationContext();
            assertThat(firstContext.getEnvironment().getProperty("consumer.http.auth-base-url")).isEqualTo("http://one.test/api");
            assertThat(firstContext.getEnvironment().getProperty("consumer.from-e2e-profile")).isEqualTo("loaded");
            assertThatThrownBy(() -> new E2eSupportImpl().mockMvc()).hasMessageContaining("ServiceContract");
            assertThat(firstContext.getBean(MockMvcJournal.class).bind(ServiceContracts.AUTH)).isNotNull();
            first.afterTestClass();
            integration.beforeTestClass();
            assertThat(new E2eSupportImpl().mockMvc()).isNotNull();
            assertThatThrownBy(() -> new E2eSupportImpl().mockMvc(ServiceContracts.AUTH)).hasMessageContaining("IT cannot");
            integration.afterTestClass();
            second.beforeTestClass();
            var secondContext = second.getTestContext().getApplicationContext();
            assertThat(secondContext).isNotSameAs(firstContext);
            assertThat(secondContext.getEnvironment().getProperty("consumer.http.auth-base-url")).isEqualTo("http://two.test/api");
            assertThat(secondContext.getBean(MockMvcJournal.class).bind(ServiceContracts.AUTH)).isNotNull();
            second.afterTestClass();
            first.beforeTestClass();
            assertThat(first.getTestContext().getApplicationContext()).isSameAs(firstContext);
            assertThatThrownBy(() -> new E2eSupportImpl().mockMvc()).hasMessageContaining("ServiceContract");
            first.afterTestClass();
        } finally {
            FeatureContextHolder.clear();
            if (previous == null) System.clearProperty("consumer.http.auth-base-url");
            else System.setProperty("consumer.http.auth-base-url", previous);
        }
    }

    @Test void invalidConfiguredTimeoutFailsAtBootstrap() {
        assertThatThrownBy(() -> new TestContextManager(InvalidTimeout.class).getTestContext().getApplicationContext())
                .hasStackTraceContaining("request-timeout must be between 1ms and 10m");
    }

    @E2E(properties = "forge-it.modules.mock-mvc.request-timeout=0ms")
    static class InvalidTimeout { E2eSupport forgeIt; }

    @E2E @IntegrationTest static class Mixed { E2eSupport forgeIt; }
    @E2E static class Unsupported { ForgeItSupport forgeIt; }
    @E2E(properties = "consumer.http.auth-base-url=http://one.test/api") static class First { E2eSupport forgeIt; }
    @E2E(properties = "consumer.http.auth-base-url=http://two.test/api") static class Second { E2eSupport forgeIt; }
    @IntegrationTest @SpringBootTest(classes = MinimalMvcConfiguration.class)
    static class MvcTest { E2eSupport forgeIt; }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @Import({JsonLoaderImpl.class, E2eSupportImpl.class})
    static class MinimalMvcConfiguration {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean MockMvc mockMvc() { return MockMvcBuilders.standaloneSetup(new Ping()).build(); }
    }
    @org.springframework.boot.test.context.TestComponent
    @RestController static class Ping { @GetMapping("/ping") String ping() { return "pong"; } }
}
