package com.sitionix.forgeit.domain.contract.clean;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.List;

public interface DbCleanupPlan {

    List<DbContract<?>> contractsFor(CleanupPhase phase);

}
