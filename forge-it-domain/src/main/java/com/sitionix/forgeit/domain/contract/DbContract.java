package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.BodySpecification;

import java.util.List;

public interface DbContract<E> {

    Class<E> entityType();

    List<DbDependency<E, ?>> dependencies();

    String defaultJsonResourceName();

    default DbContractInvocation<E> withJson(final String jsonResourceName) {
        if (jsonResourceName != null) {
            return new DbContractInvocation<>(this, BodySpecification.explicitJsonName(jsonResourceName));
        }
        return new DbContractInvocation<>(this, BodySpecification.defaultJsonName(this.defaultJsonResourceName()));
    }

    default DbContractInvocation<E> withEntity(final E entity) {
        return new DbContractInvocation<>(this, BodySpecification.entityBody(entity));
    }

    default DbContractInvocation<E> getById(final Long id) {
        return new DbContractInvocation<>(this, BodySpecification.getById(id));
    }
}


