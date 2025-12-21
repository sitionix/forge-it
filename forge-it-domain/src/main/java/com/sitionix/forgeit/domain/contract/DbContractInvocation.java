package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.BodySpecification;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class DbContractInvocation<E> {

    private final DbContract<E> contract;
    private final BodySpecification<E> bodySpecification;
    private final String label;
    private final List<DbContractInvocation<?>> children;

    public DbContractInvocation(final DbContract<E> contract,
                                final BodySpecification<E> bodySpecification) {
        this(contract, bodySpecification, null, List.of());
    }

    public DbContractInvocation(final DbContract<E> contract,
                                final BodySpecification<E> bodySpecification,
                                final String label) {
        this(contract, bodySpecification, label, List.of());
    }

    public DbContractInvocation(final DbContract<E> contract,
                                final BodySpecification<E> bodySpecification,
                                final String label,
                                final List<DbContractInvocation<?>> children) {
        this.contract = contract;
        this.bodySpecification = bodySpecification;
        this.label = label;
        this.children = List.copyOf(children);
    }

    public DbContractInvocation<E> label(final String label) {
        return new DbContractInvocation<>(this.contract, this.bodySpecification, label, this.children);
    }

    public DbContractInvocation<E> addChild(final DbContractInvocation<?> child) {
        final List<DbContractInvocation<?>> next = new ArrayList<>(this.children);
        next.add(child);
        return new DbContractInvocation<>(this.contract, this.bodySpecification, this.label, next);
    }
}
