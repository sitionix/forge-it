package com.sitionix.forgeit.consumer.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.wiremock.internal.domain.RequestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthControllerIT {

    @Autowired
    private SampleUserTests forgeit;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenUserLoginRequest_whenLogin_thenReturnLoginResponse() throws Exception {
        final Endpoint<LoginRequest, LoginResponse> endpoint = Endpoint.createContract(
                "/external/auth/login",
                HttpMethod.POST,
                LoginRequest.class,
                LoginResponse.class
        );

        final RequestBuilder<?,?> requestBuilder = this.forgeit.wiremock().createMapping(endpoint)
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
}
