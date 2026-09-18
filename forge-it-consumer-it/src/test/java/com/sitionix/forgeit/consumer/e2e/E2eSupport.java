package com.sitionix.forgeit.consumer.e2e;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;

@ForgeFeatures(MockMvcSupport.class)
public interface E2eSupport extends ForgeIT { }
