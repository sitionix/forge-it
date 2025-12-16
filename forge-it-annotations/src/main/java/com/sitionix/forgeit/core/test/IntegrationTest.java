package com.sitionix.forgeit.core.test;

import com.sitionix.forgeit.application.executor.ForgeItDbCleanupTestExecutionListener;
import com.sitionix.forgeit.application.executor.ForgeItTransactionalTestExecutionListener;
import com.sitionix.forgeit.application.executor.ForgeItTxDiagnosticTestExecutionListener;
import com.sitionix.forgeit.core.contract.DbCleanup;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(transactionManager = "transactionManager")
@Rollback
@TestExecutionListeners(
        listeners = {
                DirtiesContextBeforeModesTestExecutionListener.class,
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class,
                ForgeItTransactionalTestExecutionListener.class,
                SqlScriptsTestExecutionListener.class,
                ForgeItDbCleanupTestExecutionListener.class,
                ForgeItTxDiagnosticTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@DbCleanup(phase = CleanupPhase.NONE)
@AutoConfigureMockMvc
@Inherited
public @interface IntegrationTest {

    @AliasFor(annotation = Rollback.class, attribute = "value")
    boolean rollback() default true;

    @AliasFor(annotation = DbCleanup.class, attribute = "phase")
    CleanupPhase cleanupPhase() default CleanupPhase.NONE;
}

