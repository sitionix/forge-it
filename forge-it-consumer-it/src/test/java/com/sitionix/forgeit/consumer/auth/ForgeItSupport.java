package com.sitionix.forgeit.consumer.auth;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

@ForgeFeatures({WireMockSupport.class, MockMvcSupport.class})
public interface ForgeItSupport extends ForgeIT {
}
