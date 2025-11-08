package com.sitionix.forgeit.core.api;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.generated.ForgeITFeatures;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

/**
 * Primary entry point for ForgeIT-based test interfaces.
 * <p>
 * Exposes the feature aggregation contract generated at compile time and keeps
 * infrastructure details encapsulated within feature modules.
 */
@ForgeFeatures(WireMockSupport.class)
public interface ForgeIT extends ForgeITFeatures {
}
