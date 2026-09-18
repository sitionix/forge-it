package com.sitionix.forgeit.core.internal.test;

import org.springframework.context.annotation.Configuration;

/** Explicit empty root prevents application-under-test discovery. */
@Configuration(proxyBeanMethods = false)
public class E2eTestConfiguration { }
