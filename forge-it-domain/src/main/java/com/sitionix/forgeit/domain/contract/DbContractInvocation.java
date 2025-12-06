package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.BodySpecification;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class DbContractInvocation<E> {

    private final DbContract<E> contract;
    private final BodySpecification<E> bodySpecification;
}
