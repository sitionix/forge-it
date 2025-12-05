package com.sitionix.forgeit.domain.contract;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DbContractInvocation<E> {

    private final DbContract<E> contract;
    private final String jsonResourceName;

    public DbContract<E> contract() {
        return this.contract;
    }

    public String jsonResourceName() {
        return this.jsonResourceName;
    }
}
