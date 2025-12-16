package com.sitionix.forgeit.domain.model.sql;


public record SqlScriptDescriptor(
        ScriptPhase phase,
        int order,
        String path
) {
}