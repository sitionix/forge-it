package com.sitionix.forgeit.domain.model.sql;

import java.util.List;

public interface DbRetrieveFactory {

    <E> E getById(Class<E> entityClass, Object id);

    <E> List<E> getAll(Class<E> entityClass);

    default <E> DbRetriever<E> forClass(final Class<E> entityClass) {
        return new DbRetriever<E>() {

            @Override
            public E getById(final Object id) {
                return DbRetrieveFactory.this.getById(entityClass, id);
            }

            @Override
            public List<E> getAll() {
                return DbRetrieveFactory.this.getAll(entityClass);
            }
        };
    }
}

