package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.JsonBodySpec;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DbContractInvocation<E> {

    private final DbContract<E> contract;
    private final JsonBodySpec jsonBodySpec;

    public DbContract<E> contract() {
        return this.contract;
    }

    public JsonBodySpec jsonBodySpec() {
        return this.jsonBodySpec;
    }
}
