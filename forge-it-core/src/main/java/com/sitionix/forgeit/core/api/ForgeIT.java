package com.sitionix.forgeit.core.api;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.internal.BaselineForgeITFeature;
import com.sitionix.forgeit.core.generated.ForgeITFeatures;

/**
 * Primary entry point for ForgeIT-based test interfaces.
 * <p>
 * Exposes the feature aggregation contract generated at compile time and keeps
 * infrastructure details encapsulated within feature modules.
 */
@ForgeFeatures(BaselineForgeITFeature.class)
public interface ForgeIT extends ForgeITFeatures {
}
