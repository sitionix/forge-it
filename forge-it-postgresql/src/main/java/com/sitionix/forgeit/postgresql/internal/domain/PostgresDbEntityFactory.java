package com.sitionix.forgeit.postgresql.internal.domain;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.body.BodySpecification;
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
        final DbContract<E> contract = invocation.getContract();
        final Class<E> entityType = contract.entityType();

        return this.getEntityFromSource(invocation.getBodySpecification(), entityType);
    }

    private <E> E getEntityFromSource(final BodySpecification<E> bodySpecification, final Class<E> entityType) {
        return switch (bodySpecification.getSource()) {
            case JSON_DEFAULT -> this.getEntityFromLoader(
                    this.properties.getPaths().getEntity().getDefaults(),
                    bodySpecification.getResourceName(),
                    entityType);
            case JSON -> this.getEntityFromLoader(
                    this.properties.getPaths().getEntity().getCustom(),
                    bodySpecification.getResourceName(),
                    entityType);
            case ENTITY -> bodySpecification.getEntity();
            case GET_BY_ID -> this.entityManager.find(entityType, bodySpecification.getId());
        };
    }

    private <E> E getEntityFromLoader(final String path, final String resourceName, final Class<E> entityType) {
        this.jsonLoader.setBasePath(path);
        return this.jsonLoader.getFromFile(resourceName, entityType);
    }
}
