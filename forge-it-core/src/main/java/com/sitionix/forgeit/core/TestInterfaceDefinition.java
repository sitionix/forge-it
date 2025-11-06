package com.sitionix.forgeit.core;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

@ForgeFeatures(value = WireMockSupport.class, exposedName = "TestInterface")
interface TestInterfaceDefinition extends ForgeIT {
}
