package com.sitionix.forgeit.consumer.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AuthClient(
            RestTemplate restTemplate,
            @Value("${forgeit.wiremock.host}") String host,
            @Value("${forgeit.wiremock.port}") int port
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = "http://" + host + ":" + port;
    }

    public ResponseEntity<LoginResponse> login(final LoginRequest request) {
        return this.restTemplate.postForEntity(this.baseUrl + "/external/auth/login", request, LoginResponse.class);
    }
}
