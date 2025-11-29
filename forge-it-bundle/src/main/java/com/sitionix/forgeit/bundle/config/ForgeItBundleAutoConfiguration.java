package com.sitionix.forgeit.bundle.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that exposes ForgeIT application-level components when the bundle
 * is present on the classpath. This keeps secondary adapters (e.g. WireMock) decoupled
 * from concrete implementations while ensuring Spring can discover them through
 * component scanning.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "com.sitionix.forgeit.application")
public class ForgeItBundleAutoConfiguration {
}
