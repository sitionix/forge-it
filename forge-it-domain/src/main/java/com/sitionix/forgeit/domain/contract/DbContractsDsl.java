package com.sitionix.forgeit.domain.contract;


import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class DbContractsDsl {

    private DbContractsDsl() {
    }

    public static <E> DbContractBuilder<E> entity(final Class<E> entityType) {
        return new DefaultDbContractBuilder<>(entityType);
    }

    public interface DbContractBuilder<E> {

        <P> DbContractBuilder<E> dependsOn(
                DbContract<P> parent,
                BiConsumer<E, P> attach
        );

        <P> DbContractBuilder <E> withDefaultBody(String jsonNome);

        DbContractBuilder<E> cleanupPolicy(CleanupPolicy policy);

        DbContract<E> build();
    }

    private static final class DefaultDbContractBuilder<E> implements DbContractBuilder<E> {

        private final Class<E> entityType;
        private final List<DbDependency<E, ?>> dependencies = new ArrayList<>();
        private String defaultJsonResourceName;
        private CleanupPolicy cleanupPolicy = CleanupPolicy.DELETE_ALL;

        private DefaultDbContractBuilder(final Class<E> entityType) {
            this.entityType = entityType;
        }

        @Override
        public <P> DbContractBuilder<E> dependsOn(
                final DbContract<P> parent,
                final BiConsumer<E, P> attach
        ) {
            this.dependencies.add(new DbDependency<>(parent, attach));
            return this;
        }

        @Override
        public <P> DbContractBuilder<E> withDefaultBody(final String jsonNome) {
            this.defaultJsonResourceName = jsonNome;
            return this;
        }

        @Override
        public DbContractBuilder<E> cleanupPolicy(final CleanupPolicy policy) {
            this.cleanupPolicy = policy;
            return this;
        }

        @Override
        public DbContract<E> build() {
            final List<DbDependency<E, ?>> immutableDeps = List.copyOf(this.dependencies);
            return new DefaultDbContract<>(this.entityType,
                    immutableDeps,
                    this.defaultJsonResourceName,
                    this.cleanupPolicy);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class DefaultDbContract<E> implements DbContract<E> {

        private final Class<E> entityType;
        private final List<DbDependency<E, ?>> dependencies;
        private final String defaultJsonResourceName;
        private final CleanupPolicy cleanupPolicy;

        @Override
        public Class<E> entityType() {
            return this.entityType;
        }

        @Override
        public List<DbDependency<E, ?>> dependencies() {
            return this.dependencies;
        }

        @Override
        public String defaultJsonResourceName() {
            return this.defaultJsonResourceName;
        }

        @Override
        public CleanupPolicy cleanupPolicy() {
            return this.cleanupPolicy;
        }
    }
}
