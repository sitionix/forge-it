package com.sitionix.forgeit.core.internal;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

/**
 * Internal interface that ensures the generated {@code ForgeITFeatures} facade
 * includes the built-in feature mixins shipped with the ForgeIT distribution.
 * <p>
 * The annotation processor merges every declared feature into the
 * {@code com.sitionix.forgeit.core.generated.ForgeITFeatures} interface, which
 * is then extended by {@link com.sitionix.forgeit.core.api.ForgeIT}. Keeping
 * this interface package-private prevents it from leaking into the public API
 * surface while still triggering the annotation processor during compilation.
 */
@ForgeFeatures(WireMockSupport.class)
interface CoreFeatureAggregator extends ForgeIT {
}
