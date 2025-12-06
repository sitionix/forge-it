package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresDbEntityFactory implements DbEntityFactory {

    private final EntityManager entityManager;

    @Override
    public <E> E create(final DbContractInvocation<E> invocation) {
        final DbContract<E> contract = invocation.contract();
        final Class<E> entityType = contract.entityType();

        final E entity = this.newInstance(entityType);

        this.entityManager.persist(entity);

        return entity;
    }

    private <E> E newInstance(final Class<E> entityType) {
        try {
            return entityType.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Cannot instantiate entity type: " + entityType.getName(),
                    ex
            );
        }
    }
}
