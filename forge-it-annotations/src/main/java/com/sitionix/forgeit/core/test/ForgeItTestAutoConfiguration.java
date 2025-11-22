package com.sitionix.forgeit.core.test;

import com.sitionix.forgeit.core.internal.test.ForgeItTestRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(ForgeItTestRegistrar.class)
public class ForgeItTestAutoConfiguration {
}
