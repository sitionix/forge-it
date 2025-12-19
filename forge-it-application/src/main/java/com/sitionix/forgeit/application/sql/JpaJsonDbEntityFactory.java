package com.sitionix.forgeit.application.sql;

import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.body.BodySpecification;
import com.sitionix.forgeit.domain.contract.body.JsonBodySource;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import com.sitionix.forgeit.domain.model.sql.RelationalModuleProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class JpaJsonDbEntityFactory implements DbEntityFactory {

    private final EntityManager entityManager;
    private final JsonLoader jsonLoader;
    private final RelationalModuleProperties properties;

    @Override
    public <E> E create(final DbContractInvocation<E> invocation) {
        final Class<E> entityType = invocation.getContract().entityType();
        final BodySpecification<E> spec = invocation.getBodySpecification();

        return this.getEntityFromSource(spec, entityType);
    }

    private <E> E getEntityFromSource(final BodySpecification<E> bodySpecification,
                                      final Class<E> entityType) {

        final JsonBodySource source = bodySpecification.getSource();

        return switch (source) {
            case JSON_DEFAULT -> this.getEntityFromLoader(
                    this.properties.getPaths().getEntity().getDefaults(),
                    bodySpecification.getResourceName(),
                    entityType
            );
            case JSON -> this.getEntityFromLoader(
                    this.properties.getPaths().getEntity().getCustom(),
                    bodySpecification.getResourceName(),
                    entityType
            );
            case ENTITY -> bodySpecification.getEntity();
            case GET_BY_ID -> this.entityManager.find(entityType, bodySpecification.getId());
        };
    }

    private <E> E getEntityFromLoader(final String basePath,
                                      final String resourceName,
                                      final Class<E> entityType) {
        this.jsonLoader.setBasePath(basePath);
        return this.jsonLoader.getFromFile(resourceName, entityType);
    }
}
