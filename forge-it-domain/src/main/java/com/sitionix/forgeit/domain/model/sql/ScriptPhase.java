package com.sitionix.forgeit.domain.model.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ScriptPhase {
    SCHEMA(1L),
    CONSTRAINTS(2L),
    DATA(3L),
    CUSTOM(4L);

    @Getter
    private final Long order;
}
