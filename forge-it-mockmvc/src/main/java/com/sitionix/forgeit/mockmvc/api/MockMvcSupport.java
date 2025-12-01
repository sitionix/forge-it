package com.sitionix.forgeit.mockmvc.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.mockmvc.internal.bridge.MockMvcBridge;

/**
 * Public contract describing Mock MVC capabilities exposed to ForgeIT clients.
 */
public interface MockMvcSupport extends FeatureSupport {

    default MockMvcBridge mockMvc() {
        return FeatureContextHolder.getBean(MockMvcBridge.class);
    }
}
