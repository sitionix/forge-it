package com.sitionix.forgeit.wiremock.internal.loader;

import com.sitionix.forgeit.domain.delegator.ScopedResourcesLoader;
import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.wiremock.internal.WireMockProperties;
import org.springframework.stereotype.Component;

@Component
public class ResponseMappingResources extends ScopedResourcesLoader {

    private final WireMockProperties wireMockProperties;

    public ResponseMappingResources(final ResourcesLoader resourcesLoader,
                                    final WireMockProperties wireMockProperties) {
        super(resourcesLoader);
        this.wireMockProperties = wireMockProperties;
    }


    @Override
    protected String getResourcePath() {
        return wireMockProperties.getMapping().getResponse();
    }
}
