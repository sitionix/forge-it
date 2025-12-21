package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.List;

public interface DbGraphResult {

    /**
     * Return the entity stored for a contract. When the same contract is invoked multiple times,
     * the most recently stored entity is returned.
     */
    <E> DbEntityHandle<E> entity(DbContract<E> contract);

    /**
     * Return a labeled entity stored for a contract. Labels are set on the invocation via
     * {@code DbContractInvocation#label(String)}. Returns an empty handle when not found.
     */
    <E> DbEntityHandle<E> entity(DbContract<E> contract, String label);

    /**
     * Return all entities stored for a contract in invocation order.
     */
    <E> List<E> entities(DbContract<E> contract);

    /**
     * Return an entity stored for a contract by invocation index.
     */
    <E> DbEntityHandle<E> entityAt(DbContract<E> contract, int index);
}
