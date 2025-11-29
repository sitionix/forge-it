package com.sitionix.forgeit.core.test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "com.sitionix.forgeit.application.loader.ResourcesLoaderImpl")
@ComponentScan(basePackages = "com.sitionix.forgeit.application")
public class ForgeItTestAutoConfiguration {
}
