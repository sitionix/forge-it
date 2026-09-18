package com.sitionix.forgeit.ros.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;

public interface RosSupport extends FeatureSupport {
    default RosMessaging ros() { return FeatureContextHolder.getBean(RosMessaging.class); }
}
