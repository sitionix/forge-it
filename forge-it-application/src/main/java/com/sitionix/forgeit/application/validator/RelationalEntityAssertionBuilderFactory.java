package com.sitionix.forgeit.application.validator;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.assertion.DbEntitiesAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertionBuilderFactory;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@ConditionalOnBean(RelationalFeatureMarker.class)
public class RelationalEntityAssertionBuilderFactory implements DbEntityAssertionBuilderFactory {

    private final DbEntityAssertions entityAssertions;
    private final DbEntityFetcher entityFetcher;

    @Override
    public <E> DbEntityAssertionBuilder<E> forEntity(final DbEntityHandle<E> handle) {
        return new EntityAssertionBuilder<>(handle, this.entityAssertions, this.entityFetcher);
    }

    @Override
    public <E> DbEntitiesAssertionBuilder<E> forContract(final DbContract<E> contract) {
        return new EntitiesAssertionBuilder<>(contract, contract.entityType(), this.entityAssertions, this.entityFetcher);
    }

    @Override
    public <E> DbEntitiesAssertionBuilder<E> forEntityType(final Class<E> entityType) {
        return new EntitiesAssertionBuilder<>(null, entityType, this.entityAssertions, this.entityFetcher);
    }
}
