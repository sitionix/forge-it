package com.sitionix.forgeit.core.test;

import com.sitionix.forgeit.core.contract.DbCleanup;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import com.sitionix.forgeit.application.executor.ForgeItDbCleanupTestExecutionListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.transaction.annotation.Transactional;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ForgeItTest
@Transactional(transactionManager = "transactionManager")
@Rollback
@TestExecutionListeners(
        listeners = ForgeItDbCleanupTestExecutionListener.class,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
@DbCleanup(phase = CleanupPhase.NONE)
public @interface ForgeItDatabaseTest {

    @AliasFor(annotation = Rollback.class, attribute = "value")
    boolean rollback() default true;

    @AliasFor(annotation = DbCleanup.class, attribute = "phase")
    CleanupPhase cleanupPhase() default CleanupPhase.NONE;
}

