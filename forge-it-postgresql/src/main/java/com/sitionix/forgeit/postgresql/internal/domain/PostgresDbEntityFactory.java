package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.body.JsonBodySource;
import com.sitionix.forgeit.domain.contract.body.JsonBodySpec;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.postgresql.internal.config.PostgresqlProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresDbEntityFactory implements DbEntityFactory {

    private final EntityManager entityManager;
    private final JsonLoader jsonLoader;
    private final PostgresqlProperties properties;

    @Override
    public <E> E create(final DbContractInvocation<E> invocation) {
        final DbContract<E> contract = invocation.contract();
        final Class<E> entityType = contract.entityType();

        final E entity = this.getEntityFromSource(invocation.jsonBodySpec(), entityType);

        this.entityManager.persist(entity);

        return entity;
    }

    private <E> E getEntityFromSource(final JsonBodySpec jsonBodySpec, final Class<E> entityType) {
        this.injectPathToLoader(jsonBodySpec.source());
        return this.jsonLoader.getFromFile(jsonBodySpec.resourceName(), entityType);
    }

    private void injectPathToLoader(final JsonBodySource source) {
        switch (source) {
            case DEFAULT -> this.jsonLoader.setBasePath(this.properties.getPaths().getEntity().getDefaults());
            case EXPLICIT -> this.jsonLoader.setBasePath(this.properties.getPaths().getEntity().getCustom());
            default -> throw new IllegalArgumentException("Unknown JsonBodySource: " + source);
        }
    }
}
