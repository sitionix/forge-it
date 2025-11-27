package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ContractProxyRegistrationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private UserInterface tools;

    @Test
    void registersContractProxyAsSingletonBean() {
        assertThat(tools).isNotNull();
        assertThat(Proxy.isProxyClass(tools.getClass())).isTrue();
        assertThat(context.getBean(UserInterface.class)).isSameAs(tools);
        assertThat(context.getBeanFactory().containsSingleton(UserInterface.class.getName())).isTrue();
    }

    @Test
    void handlesObjectMethodsOnProxy() {
        assertThat(tools.toString()).contains(UserInterface.class.getName());
        assertThat(tools.equals(tools)).isTrue();
        assertThat(tools.hashCode()).isEqualTo(System.identityHashCode(tools));
    }
}
