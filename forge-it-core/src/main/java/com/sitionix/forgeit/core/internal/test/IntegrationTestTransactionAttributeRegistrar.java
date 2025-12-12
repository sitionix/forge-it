package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;

final class IntegrationTestTransactionAttributeRegistrar {

    private static final String TRANSACTION_ATTRIBUTE_SOURCE_BEAN_NAME = "transactionAttributeSource";

    private IntegrationTestTransactionAttributeRegistrar() {
    }

    static void registerIfNecessary(final ConfigurableApplicationContext context,
                                     final Class<?> testClass) {
        if (!(context instanceof AbstractApplicationContext applicationContext)) {
            return;
        }
        if (!(applicationContext.getBeanFactory() instanceof BeanDefinitionRegistry registry)
                || registry.containsBeanDefinition(TRANSACTION_ATTRIBUTE_SOURCE_BEAN_NAME)
                || !AnnotatedElementUtils.hasAnnotation(testClass, IntegrationTest.class)) {
            return;
        }

        final RootBeanDefinition beanDefinition =
                new RootBeanDefinition(IntegrationTestTransactionAttributeSource.class);
        registry.registerBeanDefinition(TRANSACTION_ATTRIBUTE_SOURCE_BEAN_NAME, beanDefinition);
    }
}
