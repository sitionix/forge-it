package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.core.testing.fake.TestFeatureSupport;

@ForgeFeatures(TestFeatureSupport.class)
public interface UserInterface extends ForgeIT {
}
