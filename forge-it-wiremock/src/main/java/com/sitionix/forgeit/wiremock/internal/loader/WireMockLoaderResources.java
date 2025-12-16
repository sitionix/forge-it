package com.sitionix.forgeit.wiremock.internal.loader;

import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.wiremock.internal.configs.WireMockProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class WireMockLoaderResources {

    private final WireMockProperties wireMockProperties;

    private final JsonLoader jsonLoader;

    public JsonLoader mappingResponse() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getResponse());
    }

    public JsonLoader mappingRequest() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getRequest());
    }

    public JsonLoader mappingDefaultResponse() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getDefaultResponse());
    }

    public JsonLoader mappingDefaultRequest() {
        return this.getLoaderWithBasePath(this.wireMockProperties.getMapping().getDefaultRequest());
    }

    private JsonLoader getLoaderWithBasePath(final String basePath) {
        this.jsonLoader.setBasePath(basePath);
        return this.jsonLoader;
    }

}
