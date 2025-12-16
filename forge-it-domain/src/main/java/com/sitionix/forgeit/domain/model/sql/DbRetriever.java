package com.sitionix.forgeit.domain.model.sql;

import java.util.List;

public interface DbRetriever<E> {

    E getById(Object id);

    List<E> getAll();
}
