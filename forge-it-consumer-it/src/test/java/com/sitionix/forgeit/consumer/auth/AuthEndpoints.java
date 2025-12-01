package com.sitionix.forgeit.consumer.auth;

import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.EndpointDefaultsContext;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.WireMockDefault;

public final class AuthEndpoints {

    private AuthEndpoints() {
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
                context -> context.plainUrl()
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
