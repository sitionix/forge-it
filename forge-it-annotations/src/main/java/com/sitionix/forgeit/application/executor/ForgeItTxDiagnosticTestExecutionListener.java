package com.sitionix.forgeit.application.executor;

import com.sitionix.forgeit.core.diagnostics.ForgeItTxDiagnostics;
import com.sitionix.forgeit.core.diagnostics.TxProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class ForgeItTxDiagnosticTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ForgeItTxDiagnosticTestExecutionListener.class);

    @Override
    public int getOrder() {
        return 5100;
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        log.info(TxProbe.snapshot("listener-beforeTestMethod"));
    }

    @Override
    public void beforeTestExecution(final TestContext testContext) {
        log.info(TxProbe.snapshot("listener-beforeTestExecution"));
        ForgeItTxDiagnostics.logTestPhaseSnapshot(
                "ForgeItTxDiagnosticTestExecutionListener.beforeTestExecution",
                testContext
        );
    }

    @Override
    public void afterTestExecution(final TestContext testContext) {
        log.info(TxProbe.snapshot("listener-afterTestExecution"));
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        log.info(TxProbe.snapshot("listener-afterTestMethod"));
    }
}
