package com.sitionix.forgeit.domain.contract.clean;

public enum CleanupPolicy {
    NONE,
    DELETE_ALL;

    public boolean isDeletable() {
        return this == DELETE_ALL;
    }
}
