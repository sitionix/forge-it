package com.sitionix.forgeit.domain.contract;

import com.sitionix.forgeit.domain.contract.body.BodySpecification;
import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;
import com.sitionix.forgeit.domain.ForgeItConfigurationException;

import java.util.List;

public interface DbContract<E> {

    Class<E> entityType();

    List<DbDependency<E, ?>> dependencies();

    String defaultJsonResourceName();

    CleanupPolicy cleanupPolicy();

    /**
     * Fields to ignore when comparing persisted entities with JSON fixtures.
     * Defaults to none.
     */
    default List<String> fieldsToIgnoreOnMatch() {
        return List.of();
    }

    default DbContractInvocation<E> withJson(final String jsonResourceName) {
        if (jsonResourceName != null) {
            return new DbContractInvocation<>(this, BodySpecification.explicitJsonName(jsonResourceName));
        }
        final String defaultJsonResourceName = this.defaultJsonResourceName();
        if (defaultJsonResourceName == null || defaultJsonResourceName.isBlank()) {
            throw new ForgeItConfigurationException("Default JSON resource name is not configured; "
                    + "provide a default fixture or pass a custom JSON resource name.");
        }
        return new DbContractInvocation<>(this, BodySpecification.defaultJsonName(defaultJsonResourceName));
    }

    default DbContractInvocation<E> withEntity(final E entity) {
        return new DbContractInvocation<>(this, BodySpecification.entityBody(entity));
    }

    default DbContractInvocation<E> getById(final Long id) {
        return new DbContractInvocation<>(this, BodySpecification.getById(id));
    }
}
