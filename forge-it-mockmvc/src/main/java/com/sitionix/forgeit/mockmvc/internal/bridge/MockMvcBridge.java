package com.sitionix.forgeit.mockmvc.internal.bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockMvcBridge {

    private final String message;

    public MockMvcBridge(@Value("${forge-it.modules.mock-mvc.message:mock-mvc-bridge}") String message) {
        this.message = message;
    }

    public String ping() {
        return this.message;
    }
}
