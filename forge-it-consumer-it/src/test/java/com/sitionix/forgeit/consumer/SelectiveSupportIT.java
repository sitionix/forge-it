package com.sitionix.forgeit.consumer;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest(properties = {
        "forgeit.wiremock.host=localhost",
        "forgeit.wiremock.port=18080"
})
@DisplayName("Given contract without WireMock support, when context starts, then WireMock beans are not registered")
class SelectiveSupportIT {

    @Autowired
    private SelectiveForgeItSupport forgeIt;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void givenContractWithoutWireMockSupport_whenContextStarts_thenWireMockBeansAreNotRegistered() {
        assertThat(this.forgeIt).isNotNull();
        assertThat(this.applicationContext.containsBean("wireMockContainerManager")).isFalse();
    }

    @Test
    void givenContractWithoutMongoSupport_whenContextStarts_thenMongoBeansAreNotRegistered() {
        assertThat(this.forgeIt).isNotNull();
        assertThat(this.applicationContext.containsBean("mongoContainerManager")).isFalse();
        assertThat(this.applicationContext.containsBean("forgeItMongoClient")).isFalse();
    }
}
