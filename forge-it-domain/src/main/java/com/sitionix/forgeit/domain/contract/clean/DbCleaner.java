package com.sitionix.forgeit.domain.contract.clean;

import java.util.List;

public interface DbCleaner {
    void clearTables(List<Class<?>> entityTypes);
}
