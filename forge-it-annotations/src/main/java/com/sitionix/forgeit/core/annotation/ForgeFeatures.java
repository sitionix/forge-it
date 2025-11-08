package com.sitionix.forgeit.core.annotation;

import com.sitionix.forgeit.core.marker.FeatureSupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ForgeFeatures {
    Class<? extends FeatureSupport>[] value();
}
