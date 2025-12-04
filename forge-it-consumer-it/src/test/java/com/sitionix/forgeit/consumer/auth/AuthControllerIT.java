package com.sitionix.forgeit.consumer.auth;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.auth.endpoint.MockMvcEndpoint;
import com.sitionix.forgeit.consumer.auth.endpoint.WireMockEndpoint;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.wiremock.internal.domain.RequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.sitionix.forgeit.wiremock.internal.domain.Parameter.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthControllerIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.forgeIt.wiremock().reset();
    }

    @Test
    void givenUserLoginRequest_whenLogin_thenReturnLoginResponse() {
        final RequestBuilder<?, ?> requestBuilder = this.forgeIt.wiremock()
                .createMapping(WireMockEndpoint.login())
                .matchesJson("requestLoginUserWithHappyPath.json")
                .responseBody("responseLoginUserWithHappyPath.json")
                .responseStatus(HttpStatus.OK)
                .plainUrl()
                .create();

        this.forgeIt.mockMvc()
                .ping(MockMvcEndpoint.login())
                .request("loginRequest.json")
                .response("loginResponse.json")
                .status(HttpStatus.OK)
                .createAndAssert();

        requestBuilder.verify();
    }

    @Test
    void givenUserLoginRequest_whenLogin_thenReturnLoginResponseWithMutation() {
        final RequestBuilder<?, ?> requestBuilder = this.forgeIt.wiremock()
                .createMapping(WireMockEndpoint.login())
                .matchesJson("requestLoginUserWithHappyPath.json",
                        d -> {
                            d.setUsername("username");
                            d.setPassword("password");
                        })
                .responseBody("responseLoginUserWithHappyPath.json",
                        d -> d.setToken("mutated-token"))
                .responseStatus(HttpStatus.OK)
                .plainUrl()
                .create();

        this.forgeIt.mockMvc()
                .ping(MockMvcEndpoint.login())
                .request("loginRequest.json",
                        d -> {
                            d.setUsername("username");
                            d.setPassword("password");
                        })
                .response("loginResponse.json", d -> d.setToken("mutated-token"))
                .status(HttpStatus.OK)
                .createAndAssert();

        requestBuilder.verify();
    }

    @Test
    void givenUserLoginRequest_whenLogin_thenReturnDefaultLogin() {

        final RequestBuilder<?, ?> request = this.forgeIt.wiremock()
                .createMapping(WireMockEndpoint.loginDefault())
                .createDefault();

        this.forgeIt.mockMvc()
                .ping(MockMvcEndpoint.loginDefault())
                .assertDefault();

        request.verify();
    }

    @Test
    void givenUserLoginRequest_whenLogin_thenReturnDefaultLoginWithMutation() {

        final RequestBuilder<?, ?> request = this.forgeIt.wiremock()
                .createMapping(WireMockEndpoint.loginDefault())
                .createDefault(d -> d.mutateRequest(r -> {
                            r.setPassword("password");
                            r.setUsername("username");
                        })
                        .mutateResponse(res -> res.setToken("mutated-token")));

        this.forgeIt.mockMvc()
                .ping(MockMvcEndpoint.loginDefault())
                .assertDefault(d -> d.mutateRequest(r -> {
                    r.setPassword("password");
                    r.setUsername("username");
                }).mutateResponse(res -> res.setToken("mutated-token")));

        request.verify();
    }

    @Test
    void givenPingRequest_whenNoBodyAndNoResponse_thenVerifyInvocation() throws Exception {
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
    void givenTokenRequest_whenQueryParametersProvided_thenReturnResponse() throws Exception {
        final RequestBuilder<?, ?> requestBuilder = this.forgeIt.wiremock().createMapping(WireMockEndpoint.token())
                .responseBody("responseTokenWithQuery.json")
                .responseStatus(HttpStatus.OK)
                .urlWithQueryParam(Map.of(
                        "username", equalTo("john.doe"),
                        "correlationId", equalTo("abc-123")
                ))
                .create();

        this.mockMvc.perform(get("/auth/token")
                        .param("username", "john.doe")
                        .param("correlationId", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-with-query"));

        requestBuilder.verify();
    }

    @Test
    void givenUserRequest_whenPathParametersProvided_thenResponseMatches() throws Exception {
        final RequestBuilder<?, ?> requestBuilder = this.forgeIt.wiremock().createMapping(WireMockEndpoint.userProfile())
                .responseBody("responseUserProfile.json")
                .responseStatus(HttpStatus.OK)
                .pathPattern(Map.of(
                        "tenantId", equalTo("tenant-1"),
                        "userId", equalTo("42")
                ))
                .create();

        this.mockMvc.perform(get("/auth/tenants/{tenantId}/users/{userId}", "tenant-1", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.username").value("john.doe"));

        requestBuilder.verify();
    }
}
