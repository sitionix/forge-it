package com.sitionix.forgeit.bundle.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration that exposes ForgeIT application-level components when the bundle
 * is present on the classpath. This keeps secondary adapters (e.g. WireMock) decoupled
 * from concrete implementations while ensuring Spring can discover them through
 * component scanning.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.sitionix.forgeit.application")
public class ForgeItBundleAutoConfiguration {
}
