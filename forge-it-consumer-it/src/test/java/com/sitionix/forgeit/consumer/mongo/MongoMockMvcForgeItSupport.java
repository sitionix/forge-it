package com.sitionix.forgeit.consumer.mongo;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import com.sitionix.forgeit.mongodb.api.MongoSupport;

@ForgeFeatures({MongoSupport.class, MockMvcSupport.class})
public interface MongoMockMvcForgeItSupport extends ForgeIT {
}
