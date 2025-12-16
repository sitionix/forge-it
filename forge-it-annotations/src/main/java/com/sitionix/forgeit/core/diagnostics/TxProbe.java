package com.sitionix.forgeit.core.diagnostics;

public final class TxProbe {

    private TxProbe() {
    }

    public static String describe(final Object bean) {
        if (bean == null) {
            return "null";
        }
        return bean.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(bean));
    }

    public static String snapshot(final String where) {
        return String.format(
                "[TX-PROBE] %s thread=%s actual=%s sync=%s name=%s resources=%s",
                where,
                Thread.currentThread().getName(),
                org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive(),
                org.springframework.transaction.support.TransactionSynchronizationManager.getCurrentTransactionName(),
                org.springframework.transaction.support.TransactionSynchronizationManager.getResourceMap().keySet()
        );
    }
}
