package com.sitionix.forgeit.wiremock.internal.loader;

import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WireMockLoaderResources {

    private final ResourcesLoader mappingResponse;


}
