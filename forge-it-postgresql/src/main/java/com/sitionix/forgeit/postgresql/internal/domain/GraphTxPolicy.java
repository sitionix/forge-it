package com.sitionix.forgeit.postgresql.internal.domain;

import org.springframework.transaction.TransactionDefinition;

public enum GraphTxPolicy {
    REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),
    REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),
    MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY);

    private final int propagationBehavior;

    GraphTxPolicy(final int propagationBehavior) {
        this.propagationBehavior = propagationBehavior;
    }

    public int propagationBehavior() {
        return this.propagationBehavior;
    }
}
