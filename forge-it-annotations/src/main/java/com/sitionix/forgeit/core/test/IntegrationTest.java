package com.sitionix.forgeit.core.test;

import com.sitionix.forgeit.application.executor.ForgeItDbCleanupTestExecutionListener;
import com.sitionix.forgeit.core.contract.DbCleanup;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ForgeItTest
@TestExecutionListeners(
        listeners = {
                ForgeItDbCleanupTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DbCleanup(phase = CleanupPhase.AFTER_EACH)
@AutoConfigureMockMvc
@Inherited
public @interface IntegrationTest {

    @AliasFor(annotation = DbCleanup.class, attribute = "phase")
    CleanupPhase cleanupPhase() default CleanupPhase.AFTER_EACH;
}

