package com.sitionix.forgeit.consumer.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "forge-it.modules.wiremock", name = "enabled", havingValue = "true")
public class AuthClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AuthClient(
            RestTemplate restTemplate,
            @Value("${forgeit.wiremock.host:localhost}") String host,
            @Value("${forgeit.wiremock.port:8089}") int port
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = "http://" + host + ":" + port;
    }

    public ResponseEntity<LoginResponse> login(final LoginRequest request) {
        return this.restTemplate.postForEntity(this.baseUrl + "/external/auth/login", request, LoginResponse.class);
    }

    public ResponseEntity<Void> ping() {
        return this.restTemplate.getForEntity(this.baseUrl + "/external/auth/ping", Void.class);
    }

    public ResponseEntity<LoginResponse> requestToken(final String username, final String correlationId) {
        final String uri = UriComponentsBuilder.fromHttpUrl(this.baseUrl + "/external/auth/token")
                .queryParam("username", username)
                .queryParam("correlationId", correlationId)
                .toUriString();

        return this.restTemplate.getForEntity(uri, LoginResponse.class);
    }

    public ResponseEntity<UserProfileResponse> fetchUser(
            final String tenantId,
            final String userId
    ) {
        final String uri = UriComponentsBuilder
                .fromHttpUrl(this.baseUrl + "/external/tenants/{tenantId}/users/{userId}")
                .buildAndExpand(tenantId, userId)
                .toUriString();

        return this.restTemplate.getForEntity(uri, UserProfileResponse.class);
    }
}
