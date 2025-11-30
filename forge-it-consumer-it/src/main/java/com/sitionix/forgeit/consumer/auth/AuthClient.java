package com.sitionix.forgeit.consumer.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthClient {

    private final RestTemplate restTemplate;
    private final String externalBaseUrl;

    public AuthClient(final RestTemplate restTemplate,
                      @Value("${auth.external.base-url:http://localhost:8089}") final String externalBaseUrl) {
        this.restTemplate = restTemplate;
        this.externalBaseUrl = externalBaseUrl;
    }

    public ResponseEntity<LoginResponse> login(final LoginRequest request) {
        return this.restTemplate.postForEntity(this.externalBaseUrl + "/external/auth/login", request, LoginResponse.class);
    }
}
