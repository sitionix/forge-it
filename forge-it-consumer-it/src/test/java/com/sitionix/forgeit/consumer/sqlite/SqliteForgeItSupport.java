package com.sitionix.forgeit.consumer.sqlite;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.sqlite.api.SqliteSupport;

@ForgeFeatures(SqliteSupport.class)
public interface SqliteForgeItSupport extends ForgeIT {
}
