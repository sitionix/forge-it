package com.sitionix.forgeit.mockmvc.internal.executor;

import java.time.Duration;

/** Complete transport response; elapsed excludes fixture loading and assertions. */
public record MockMvcResponse(int status, String body, Duration elapsed) {
}
