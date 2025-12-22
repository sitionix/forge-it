package com.sitionix.forgeit.domain.model.sql;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.List;

public interface DbEntityFetcher {

    <E> E reloadById(DbContract<E> contract, Object id);

    <E> E reloadByIdWithRelations(DbContract<E> contract, Object id);

    <E> List<E> loadAll(Class<E> entityType);

    <E> List<E> loadAllWithRelations(Class<E> entityType);
}
