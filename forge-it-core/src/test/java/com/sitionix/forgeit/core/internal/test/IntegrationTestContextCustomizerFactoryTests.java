package com.sitionix.forgeit.core.internal.test;

import java.util.List;

import com.sitionix.forgeit.core.testing.UserInterface;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationTestContextCustomizerFactoryTests {

    private final IntegrationTestContextCustomizerFactory factory =
            new IntegrationTestContextCustomizerFactory();

    @Test
    void returnsNullWhenNoForgeItContractsPresent() {
        final ContextCustomizer customizer = this.factory.createContextCustomizer(
                WithoutContracts.class, List.of());

        assertThat(customizer).isNull();
    }

    @Test
    void createsCustomizerWhenForgeItContractPresent() {
        final ContextCustomizer customizer = this.factory.createContextCustomizer(
                WithForgeItContract.class, List.of());

        assertThat(customizer)
                .isEqualTo(new ForgeIntegrationTestContextCustomizer(
                        UserInterface.class, List.of(WireMockSupport.class)));
    }

    @SuppressWarnings("unused")
    private static final class WithoutContracts {
    }

    @SuppressWarnings("unused")
    private static final class WithForgeItContract {

        private UserInterface forgeItContract;
    }
}
