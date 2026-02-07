package com.sitionix.forgeit.consumer.wiremock;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.auth.endpoint.WireMockEndpoint;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest(preparations = AuthPingPreparation.class)
class WireMockDataPreparationIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given data preparation When test runs Then WireMock mapping is ready")
    void givenDataPreparation_whenTestRuns_thenWireMockMappingIsReady() throws Exception {
        this.mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isNoContent());

        this.forgeIt.wiremock()
                .check(WireMockEndpoint.ping())
                .verify();
    }

    @Test
    @DisplayName("Given data preparation When another test runs Then WireMock mapping is ready")
    void givenDataPreparation_whenAnotherTestRuns_thenWireMockMappingIsReady() throws Exception {
        this.mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isNoContent());

        this.forgeIt.wiremock()
                .check(WireMockEndpoint.ping())
                .verify();
    }
}
