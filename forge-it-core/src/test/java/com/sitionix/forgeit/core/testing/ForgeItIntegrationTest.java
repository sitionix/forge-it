package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ForgeItIntegrationTest {

    @Autowired
    private UserInterface tools;

    @Test
    void shouldDelegateWireMockFeatureThroughProxy() {
    }
}
