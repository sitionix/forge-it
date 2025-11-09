package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

@ForgeFeatures(WireMockSupport.class)
public interface UserInterface extends ForgeIT {
}
