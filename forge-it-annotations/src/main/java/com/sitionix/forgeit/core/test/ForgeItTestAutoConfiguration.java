package com.sitionix.forgeit.core.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "com.sitionix.forgeit.application")
public class ForgeItTestAutoConfiguration {
}
