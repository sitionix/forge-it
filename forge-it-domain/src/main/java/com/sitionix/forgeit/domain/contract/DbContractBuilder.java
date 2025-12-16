package com.sitionix.forgeit.domain.contract;

import java.util.function.BiConsumer;
public interface DbContractBuilder<E> {

        /**
         * Provide JSON payload as raw string.
         */
        DbContractBuilder<E> json(String json);

        /**
         * Declare a dependency: this entity depends on parent,
         * and attach defines how to set parent into child.
         */
        <P> DbContractBuilder<E> dependsOn(
                DbContract<P> parent,
                BiConsumer<E, P> attach
        );

        DbContract<E> build();
}
