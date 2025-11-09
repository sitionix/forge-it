package com.sitionix.forgeit.core.internal.contract;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

@ForgeFeatures(WireMockSupport.class)
interface ForgeItWireMockContract extends ForgeIT {
}
