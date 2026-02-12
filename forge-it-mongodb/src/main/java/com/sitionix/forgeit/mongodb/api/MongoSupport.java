package com.sitionix.forgeit.mongodb.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.mongodb.internal.repository.MongoForge;

/**
 * Public contract describing MongoDB capabilities exposed to ForgeIT clients.
 */
public interface MongoSupport extends FeatureSupport {

    default MongoForge mongo() {
        return FeatureContextHolder.getBean(MongoForge.class);
    }
}
