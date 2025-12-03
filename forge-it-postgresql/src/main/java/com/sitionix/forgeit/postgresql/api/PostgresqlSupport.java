package com.sitionix.forgeit.postgresql.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.postgresql.internal.bridge.PostgresqlBridge;

/**
 * Public contract describing PostgreSQL capabilities exposed to ForgeIT clients.
 */
public interface PostgresqlSupport extends FeatureSupport {

    default PostgresqlBridge postgresql() {
        return FeatureContextHolder.getBean(PostgresqlBridge.class);
    }
}
