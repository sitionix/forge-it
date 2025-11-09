package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.core.testing.fake.TestFeatureTool;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@IntegrationTest(classes = ForgeItIntegrationTest.TestApplication.class)
class ForgeItIntegrationTest {

    @Autowired
    private UserInterface tools;

    @Test
    void shouldDelegateFeatureCallsThroughProxy() {
        final TestFeatureTool featureTool = tools.testFeature();
        final String response = featureTool.ping("value");

        Assertions.assertThat(response).isEqualTo("pong:value");
        Assertions.assertThat(featureTool.invocationCount()).isEqualTo(1);
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
