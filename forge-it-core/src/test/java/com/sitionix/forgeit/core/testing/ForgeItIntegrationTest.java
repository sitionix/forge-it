package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@IntegrationTest(classes = ForgeItIntegrationTest.TestApplication.class)
class ForgeItIntegrationTest {

    @Autowired
    private UserInterface tools;

    @Test
    void shouldExposeWireMockFeature() {
        Assertions.assertThat(tools.wiremock()).isEqualTo("wiremock");
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
