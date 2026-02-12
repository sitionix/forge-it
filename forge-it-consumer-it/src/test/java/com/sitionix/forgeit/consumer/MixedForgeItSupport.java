package com.sitionix.forgeit.consumer;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import com.sitionix.forgeit.mongodb.api.MongoSupport;
import com.sitionix.forgeit.postgresql.api.PostgresqlSupport;

@ForgeFeatures({MockMvcSupport.class, PostgresqlSupport.class, MongoSupport.class})
public interface MixedForgeItSupport extends ForgeIT {
}
