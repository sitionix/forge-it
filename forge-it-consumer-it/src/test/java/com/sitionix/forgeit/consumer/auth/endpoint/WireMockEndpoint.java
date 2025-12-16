package com.sitionix.forgeit.consumer.auth.endpoint;

import com.sitionix.forgeit.consumer.auth.LoginRequest;
import com.sitionix.forgeit.consumer.auth.LoginResponse;
import com.sitionix.forgeit.consumer.auth.UserProfileResponse;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.wiremock.WiremockDefault;

public final class WireMockEndpoint {

    private WireMockEndpoint() {
    }

    public static Endpoint<LoginRequest, LoginResponse> login() {
        return Endpoint.createContract(
                "/external/auth/login",
                HttpMethod.POST,
                LoginRequest.class,
                LoginResponse.class
        );
    }

    public static Endpoint<LoginRequest, LoginResponse> loginDefault() {
        return Endpoint.createContract(
                "/external/auth/login",
                HttpMethod.POST,
                LoginRequest.class,
                LoginResponse.class,
                (WiremockDefault) context -> context.plainUrl()
                       .matchesJson("requestLoginUserWithHappyPath.json")
                       .responseBody("responseLoginUserWithHappyPath.json")
                       .responseStatus(200)
        );
    }

    public static Endpoint<Void, Void> ping() {
        return Endpoint.createContract(
                "/external/auth/ping",
                HttpMethod.GET,
                Void.class,
                Void.class
        );
    }

    public static Endpoint<Void, LoginResponse> token() {
        return Endpoint.createContract(
                "/external/auth/token",
                HttpMethod.GET,
                Void.class,
                LoginResponse.class
        );
    }

    public static Endpoint<Void, UserProfileResponse> userProfile() {
        return Endpoint.createContract(
                "/external/tenants/{tenantId}/users/{userId}",
                HttpMethod.GET,
                Void.class,
                UserProfileResponse.class
        );
    }
}
