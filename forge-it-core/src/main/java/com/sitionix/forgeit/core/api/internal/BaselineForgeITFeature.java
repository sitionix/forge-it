package com.sitionix.forgeit.core.api.internal;

/**
 * Marker feature that keeps the ForgeIT core module's feature aggregation
 * pipeline active during compilation.
 * <p>
 * The annotation processor uses this interface when generating the base
 * {@code ForgeITFeatures} contract so that consumers can extend {@code ForgeIT}
 * without pulling in any infrastructure concerns by default. This type lives in
 * the internal package on purpose and should not be referenced by consumers.
*/
public interface BaselineForgeITFeature {
}
