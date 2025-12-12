package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.ResourcelessTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class IntegrationTestTransactionListenerIT {

    @SuppressWarnings("FieldCanBeLocal")
    private final UserInterface forgeIt = null;

    @Test
    void shouldStartTransactionForIntegrationTestClass() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TransactionManagerConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new ResourcelessTransactionManager();
        }
    }
}
