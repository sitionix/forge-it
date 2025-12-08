package com.sitionix.forgeit.application.sql;

import com.sitionix.forgeit.domain.model.sql.DbRetrieveFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DbRetrieveFactoryIml implements DbRetrieveFactory {

    private final EntityManager entityManager;

    @Override
    public <E> E getById(final Class<E> entityClass, final Object id) {
        return this.entityManager.find(entityClass, id);
    }

    @Override
    public <E> List<E> getAll(final Class<E> entityClass) {
        return this.entityManager
                .createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }
}
