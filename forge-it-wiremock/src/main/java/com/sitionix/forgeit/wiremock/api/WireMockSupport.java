package com.sitionix.forgeit.wiremock.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;

/**
 * Public contract describing WireMock capabilities exposed to ForgeIT clients.
 */
public interface WireMockSupport extends FeatureSupport {

    default WireMockJournal wiremock() {
        return FeatureContextHolder.getBean(WireMockJournal.class);
    }
}
