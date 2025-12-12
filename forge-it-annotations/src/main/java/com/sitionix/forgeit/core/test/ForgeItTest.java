package com.sitionix.forgeit.core.test;


import com.sitionix.forgeit.core.internal.test.ForgeItTestRegistrar;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles(ActiveProfile.IT)
@Import({ForgeItTestAutoConfiguration.class, ForgeItTestRegistrar.class})
public @interface ForgeItTest {
}
