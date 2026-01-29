package com.sitionix.forgeit.consumer.auth;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.auth.endpoint.MockMvcEndpoint;
import com.sitionix.forgeit.consumer.auth.endpoint.WireMockEndpoint;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.mockmvc.api.QueryParams;
import com.sitionix.forgeit.wiremock.api.WireMockQueryParams;
import com.sitionix.forgeit.wiremock.internal.domain.RequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.sitionix.forgeit.wiremock.api.Parameter.equalTo;

@IntegrationTest
class WireMockRequestMatchingIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @BeforeEach
    void setUp() {
        this.forgeIt.wiremock().reset();
    }

    @Test
    void givenDifferentQueryRequests_whenVerifyingByQuery_thenEachIsCountedOnce() {
        //given
        final RequestBuilder<?, ?> firstRequest = this.forgeIt.wiremock().createMapping(WireMockEndpoint.token())
                .responseBody("responseTokenWithQuery.json")
                .responseStatus(HttpStatus.OK)
                .urlWithQueryParam(WireMockQueryParams.create()
                        .add("username", equalTo("john.doe"))
                        .add("correlationId", equalTo("abc-123")))
                .create();

        final RequestBuilder<?, ?> secondRequest = this.forgeIt.wiremock().createMapping(WireMockEndpoint.token())
                .responseBody("responseTokenWithQuery.json")
                .responseStatus(HttpStatus.OK)
                .urlWithQueryParam(WireMockQueryParams.create()
                        .add("username", equalTo("jane.doe"))
                        .add("correlationId", equalTo("xyz-789")))
                .create();

        //when
        this.forgeIt.mockMvc()
                .ping(MockMvcEndpoint.token())
                .withQueryParameters(QueryParams.create()
                        .add("username", "john.doe")
                        .add("correlationId", "abc-123"))
                .expectResponse("responseTokenWithQuery.json")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        this.forgeIt.mockMvc()
                .ping(MockMvcEndpoint.token())
                .withQueryParameters(QueryParams.create()
                        .add("username", "jane.doe")
                        .add("correlationId", "xyz-789"))
                .expectResponse("responseTokenWithQuery.json")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        firstRequest.verify();
        secondRequest.verify();
    }
}
