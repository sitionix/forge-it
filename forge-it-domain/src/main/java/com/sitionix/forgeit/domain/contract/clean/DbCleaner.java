package com.sitionix.forgeit.domain.contract.clean;

import com.sitionix.forgeit.domain.contract.DbContract;

import java.util.List;

public interface DbCleaner {
    void clearTables(List<DbContract<?>> contracts);
}
