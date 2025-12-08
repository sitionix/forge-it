package com.sitionix.forgeit.core.test;

import com.sitionix.forgeit.core.internal.test.ForgeItTestRegistrar;
import jakarta.transaction.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.Rollback;
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
@AutoConfigureMockMvc
@Transactional
@Rollback
@Import({ForgeItTestAutoConfiguration.class, ForgeItTestRegistrar.class})
public @interface IntegrationTest {

    /**
     * Controls whether test transactions should be rolled back.
     * By default it's true (standard Spring Test behavior).
     */
    @AliasFor(annotation = Rollback.class, attribute = "value")
    boolean rollback() default true;

}
