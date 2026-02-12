package com.sitionix.forgeit.domain.model.sql;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

final class DbFilteredRetriever<E> implements DbRetriever<E> {

    private final DbRetriever<E> delegate;
    private final Predicate<E> predicate;

    DbFilteredRetriever(final DbRetriever<E> delegate, final Predicate<E> predicate) {
        this.delegate = Objects.requireNonNull(delegate, "DbRetriever must not be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate must not be null");
    }

    @Override
    public E getById(final Object id) {
        final E entity = this.delegate.getById(id);
        if (entity == null) {
            return null;
        }
        return this.predicate.test(entity) ? entity : null;
    }

    @Override
    public List<E> getAll() {
        final List<E> entities = this.delegate.getAll();
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .filter(this.predicate)
                .toList();
    }
}
