package com.sitionix.forgeit.application.sql;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;
import com.sitionix.forgeit.domain.model.sql.DbRetrieveFactory;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class RelationalEntityFetcher implements DbEntityFetcher {

    private final DbRetrieveFactory retrieveFactory;
    private final PlatformTransactionManager transactionManager;

    @Override
    public <E> E reloadById(final DbContract<E> contract, final Object id) {
        return this.retrieveFactory.forClass(contract.entityType()).getById(id);
    }

    @Override
    public <E> E reloadByIdWithRelations(final DbContract<E> contract, final Object id) {
        final TransactionTemplate template = new TransactionTemplate(this.transactionManager);
        return template.execute(status -> {
            final E entity = this.retrieveFactory.forClass(contract.entityType()).getById(id);
            initializeDeepStructure(entity);
            return entity;
        });
    }

    @Override
    public <E> List<E> loadAll(final Class<E> entityType) {
        return this.retrieveFactory.forClass(entityType).getAll();
    }

    @Override
    public <E> List<E> loadAllWithRelations(final Class<E> entityType) {
        final TransactionTemplate template = new TransactionTemplate(this.transactionManager);
        final List<E> loaded = template.execute(status -> {
            final List<E> entities = this.retrieveFactory.forClass(entityType).getAll();
            for (final E entity : entities) {
                this.initializeDeepStructure(entity);
            }
            return entities;
        });
        return loaded == null ? new ArrayList<>() : loaded;
    }

    private void initializeDeepStructure(final Object entity) {
        if (entity == null) {
            return;
        }
        final org.springframework.beans.BeanWrapper wrapper = new org.springframework.beans.BeanWrapperImpl(entity);
        final java.beans.PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
        if (descriptors == null) {
            return;
        }
        for (final java.beans.PropertyDescriptor descriptor : descriptors) {
            final String name = descriptor.getName();
            if ("class".equals(name)) {
                continue;
            }
            final Object value = wrapper.getPropertyValue(name);
            if (value == null || this.isScalar(value)) {
                continue;
            }
            this.initializeHibernateProxy(value);
            if (value instanceof Iterable) {
                for (final Object ignored : (Iterable<?>) value) {
                    // touching the iterator triggers initialization
                }
            } else if (value.getClass().isArray()) {
                java.lang.reflect.Array.getLength(value);
            }
        }
    }

    private void initializeHibernateProxy(final Object value) {
        try {
            final Class<?> hibernate = Class.forName("org.hibernate.Hibernate");
            final java.lang.reflect.Method initialize = hibernate.getMethod("initialize", Object.class);
            initialize.invoke(null, value);
        } catch (final ReflectiveOperationException ignored) {
            // Hibernate not on the classpath or initialization failed; rely on direct access instead.
        }
    }

    private boolean isScalar(final Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }
}
