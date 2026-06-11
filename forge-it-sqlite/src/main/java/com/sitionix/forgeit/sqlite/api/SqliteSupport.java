package com.sitionix.forgeit.sqlite.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.sqlite.internal.repository.SqliteForge;

/**
 * Public contract describing SQLite capabilities exposed to ForgeIT clients.
 */
public interface SqliteSupport extends FeatureSupport {

    default SqliteForge sqlite() {
        return FeatureContextHolder.getBean(SqliteForge.class);
    }
}
