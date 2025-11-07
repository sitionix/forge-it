package com.sitionix.forgeit.core;

import com.sitionix.forgeit.core.examples.TestInterfaceDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAutowireTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void forgeInterfaceBeanIsAvailableForInjection() {
        contextRunner.run(context -> {
            TestInterfaceDefinition bean = context.getBean(TestInterfaceDefinition.class);
            assertThat(bean.wiremock()).isEqualTo("wiremock");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        TestInterfaceDefinition testInterfaceDefinition() {
            return new TestInterfaceDefinition() { };
        }
    }
}
