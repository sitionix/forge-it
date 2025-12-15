package com.sitionix.forgeit.application.executor;

import com.sitionix.forgeit.core.diagnostics.ForgeItTxDiagnostics;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class ForgeItTxDiagnosticTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 5100;
    }

    @Override
    public void beforeTestExecution(final TestContext testContext) {
        ForgeItTxDiagnostics.logTestPhaseSnapshot(
                "ForgeItTxDiagnosticTestExecutionListener.beforeTestExecution",
                testContext
        );
    }
}
