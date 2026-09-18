package com.sitionix.forgeit.mockmvc.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.domain.endpoint.ServiceContract;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.mockmvc.internal.journal.MockMvcJournal;

/**
 * Public contract describing Mock MVC capabilities exposed to ForgeIT clients.
 */
public interface MockMvcSupport extends FeatureSupport {

    default MockMvcJournal mockMvc() {
        return FeatureContextHolder.getBean(MockMvcJournal.class).requireUnbound();
    }
    default MockMvcJournal mockMvc(final ServiceContract service) {
        return FeatureContextHolder.getBean(MockMvcJournal.class).bind(service);
    }
}
