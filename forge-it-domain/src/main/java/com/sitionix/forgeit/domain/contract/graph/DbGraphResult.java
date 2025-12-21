package com.sitionix.forgeit.domain.contract.graph;

import com.sitionix.forgeit.domain.contract.DbContract;

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
}
