package com.sitionix.forgeit.consumer.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthClient authClient;

    public AuthController(final AuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody final LoginRequest request) {
        return this.authClient.login(request);
    }

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return this.authClient.ping();
    }

    @GetMapping("/token")
    public ResponseEntity<LoginResponse> token(
            @RequestParam("username") final String username,
            @RequestParam("correlationId") final String correlationId
    ) {
        return this.authClient.requestToken(username, correlationId);
    }

    @GetMapping("/tenants/{tenantId}/users/{userId}")
    public ResponseEntity<UserProfileResponse> user(
            @PathVariable("tenantId") final String tenantId,
            @PathVariable("userId") final String userId
    ) {
        return this.authClient.fetchUser(tenantId, userId);
    }
}
