package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.BodySpecification;
import lombok.Getter;

@Getter
public final class DbContractInvocation<E> {

    private final DbContract<E> contract;
    private final BodySpecification<E> bodySpecification;
    private final String label;

    public DbContractInvocation(final DbContract<E> contract,
                                final BodySpecification<E> bodySpecification) {
        this(contract, bodySpecification, null);
    }

    public DbContractInvocation(final DbContract<E> contract,
                                final BodySpecification<E> bodySpecification,
                                final String label) {
        this.contract = contract;
        this.bodySpecification = bodySpecification;
        this.label = label;
    }

    public DbContractInvocation<E> label(final String label) {
        return new DbContractInvocation<>(this.contract, this.bodySpecification, label);
    }
}
