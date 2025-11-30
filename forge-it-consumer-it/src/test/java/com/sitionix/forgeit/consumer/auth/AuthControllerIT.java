package com.sitionix.forgeit.consumer.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.wiremock.internal.domain.RequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthControllerIT {

    @Autowired
    private SampleUserTests forgeit;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.forgeit.wiremock().reset();
    }

    @Test
    void givenUserLoginRequest_whenLogin_thenReturnLoginResponse() throws Exception {
        final RequestBuilder<?,?> requestBuilder = this.forgeit.wiremock().createMapping(AuthEndpoints.login())
                .matchesJson("requestLoginUserWithHappyPath.json")
                .responseBody("responseLoginUserWithHappyPath.json")
                .responseStatus(HttpStatus.OK)
                .plainUrl()
                .create();

        final LoginRequest loginRequest = new LoginRequest("john.doe", "s3cr3t");

        this.mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        requestBuilder.verify();
    }

    @Test
    void givenPingRequest_whenNoBodyAndNoResponse_thenVerifyInvocation() throws Exception {
        final RequestBuilder<?, ?> requestBuilder = this.forgeit.wiremock().createMapping(AuthEndpoints.ping())
                .responseStatus(HttpStatus.NO_CONTENT)
                .plainUrl()
                .create();

        this.mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isNoContent());

        requestBuilder.verify();
    }

    @Test
    void givenTokenRequest_whenQueryParametersProvided_thenReturnResponse() throws Exception {
        final RequestBuilder<?, ?> requestBuilder = this.forgeit.wiremock().createMapping(AuthEndpoints.token())
                .responseBody("responseTokenWithQuery.json")
                .responseStatus(HttpStatus.OK)
                .urlWithQueryParam(Map.of(
                        "username", "john.doe",
                        "correlationId", "abc-123"
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
        final RequestBuilder<?, ?> requestBuilder = this.forgeit.wiremock().createMapping(AuthEndpoints.userProfile())
                .responseBody("responseUserProfile.json")
                .responseStatus(HttpStatus.OK)
                .pathPattern(Map.of(
                        "tenantId", "tenant-1",
                        "userId", "42"
                ))
                .create();

        this.mockMvc.perform(get("/auth/tenants/{tenantId}/users/{userId}", "tenant-1", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.username").value("john.doe"));

        requestBuilder.pathWithParameters(Map.of(
                "tenantId", "tenant-1",
                "userId", "42"
        )).verify();
    }
}
