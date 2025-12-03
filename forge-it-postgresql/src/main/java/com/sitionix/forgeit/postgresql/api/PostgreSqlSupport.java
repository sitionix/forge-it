package com.sitionix.forgeit.postgresql.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.postgresql.internal.bridge.PostgreSqlBridge;

/**
 * Public contract describing PostgreSQL capabilities exposed to ForgeIT clients.
 */
public interface PostgreSqlSupport extends FeatureSupport {

    default PostgreSqlBridge postgresql() {
        return FeatureContextHolder.getBean(PostgreSqlBridge.class);
    }
}
