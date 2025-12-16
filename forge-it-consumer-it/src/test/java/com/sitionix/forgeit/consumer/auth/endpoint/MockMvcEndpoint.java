package com.sitionix.forgeit.consumer.auth.endpoint;

import com.sitionix.forgeit.consumer.auth.LoginRequest;
import com.sitionix.forgeit.consumer.auth.LoginResponse;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;

public class MockMvcEndpoint {

    public static Endpoint<LoginRequest, LoginResponse> login() {
        return Endpoint.createContract(
                "/auth/login",
                HttpMethod.POST,
                LoginRequest.class,
                LoginResponse.class
        );
    }

    public static Endpoint<LoginRequest, LoginResponse> loginDefault() {
        return Endpoint.createContract(
                "/auth/login",
                HttpMethod.POST,
                LoginRequest.class,
                LoginResponse.class,
                (MockmvcDefault) context -> context
                        .request("loginRequest.json")
                        .response("loginResponse.json")
                        .status(200)
        );
    }
}
