package com.sitionix.forgeit.consumer.wiremock;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.auth.endpoint.WireMockEndpoint;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.wiremock.internal.domain.RequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class WireMockResetIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given a WireMock mapping When first test runs Then request count is isolated")
    void givenWireMockMapping_whenFirstTestRuns_thenRequestCountIsIsolated() throws Exception {
        final RequestBuilder<?, ?> requestBuilder = this.forgeIt.wiremock()
                .createMapping(WireMockEndpoint.ping())
                .responseStatus(HttpStatus.NO_CONTENT)
                .plainUrl()
                .create();

        this.mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isNoContent());

        requestBuilder.verify();
    }

    @Test
    @DisplayName("Given the same WireMock mapping When another test runs Then request count resets")
    void givenSameWireMockMapping_whenAnotherTestRuns_thenRequestCountResets() throws Exception {
        final RequestBuilder<?, ?> requestBuilder = this.forgeIt.wiremock()
                .createMapping(WireMockEndpoint.ping())
                .responseStatus(HttpStatus.NO_CONTENT)
                .plainUrl()
                .create();

        this.mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isNoContent());

        requestBuilder.verify();
    }
}
