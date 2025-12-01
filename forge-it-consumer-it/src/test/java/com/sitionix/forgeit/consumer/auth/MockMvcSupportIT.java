package com.sitionix.forgeit.consumer.auth;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class MockMvcSupportIT {

    @Autowired
    private ForgeItSupport forgeit;

    @Test
    void shouldExposeMockMvcBridgeFromGeneratedSupport() {
        Assertions.assertThat(this.forgeit.mockMvc().ping()).isEqualTo("mock-mvc-bridge");
    }
}
