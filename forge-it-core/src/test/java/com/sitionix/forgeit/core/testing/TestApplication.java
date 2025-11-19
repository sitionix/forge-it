package com.sitionix.forgeit.core.testing;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot configuration used to bootstrap the test application context.
 * The class lives in the test scope only and acts as the anchor for component scanning
 * during integration tests.
 */
@SpringBootApplication
public class TestApplication {
}
