package com.sitionix.forgeit.consumer.auth;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.mockmvc.api.MockMvcSupport;
import com.sitionix.forgeit.postgresql.api.PostgreSqlSupport;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

@ForgeFeatures({WireMockSupport.class, MockMvcSupport.class, PostgreSqlSupport.class})
public interface ForgeItSupport extends ForgeIT {
}
