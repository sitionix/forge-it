package com.sitionix.forgeit.wiremock.internal.loader;

import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.wiremock.internal.configs.WireMockProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class WireMockLoaderResources {

    private final WireMockProperties wireMockProperties;

    private final ResourcesLoader resourcesLoader;

    public ResourcesLoader mappingResponse() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getResponse());
    }

    public ResourcesLoader mappingRequest() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getRequest());
    }

    public ResourcesLoader mappingDefaultResponse() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getDefaultResponse());
    }

    public ResourcesLoader mappingDefaultRequest() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getDefaultRequest());
    }

    private ResourcesLoader getLoaderWithBasePath(final String basePath) {
        this.resourcesLoader.setBasePath(basePath);
        return this.resourcesLoader;
    }

}
