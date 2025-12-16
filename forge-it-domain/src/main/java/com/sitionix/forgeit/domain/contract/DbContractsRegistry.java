package com.sitionix.forgeit.domain.contract;


import java.util.List;

public interface DbContractsRegistry {

    /**
     * Returns all known DbContracts discovered in the project.
     */
    List<DbContract<?>> allContracts();
}
