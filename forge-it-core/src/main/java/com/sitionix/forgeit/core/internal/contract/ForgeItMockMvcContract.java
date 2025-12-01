package com.sitionix.forgeit.core.internal.contract;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;

@ForgeFeatures(MockMvcSupport.class)
interface ForgeItMockMvcContract extends ForgeIT {
}
