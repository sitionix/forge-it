package com.sitionix.forgeit.mockmvc.internal.loader;

import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockMvcLoader {

    private final ResourcesLoader resourcesLoader;

    private final MockMvcProperties props;

    public ResourcesLoader mvcRequest() {
        return this.getLoaderWithBasePath(this.props.getPath().getRequest());
    }

    public ResourcesLoader mvcResponse() {
        return this.getLoaderWithBasePath(this.props.getPath().getResponse());
    }

    public ResourcesLoader mvcDefaultRequest() {
        return this.getLoaderWithBasePath(this.props.getPath().getDefaultRequest());
    }

    public ResourcesLoader mvcDefaultResponse() {
        return this.getLoaderWithBasePath(this.props.getPath().getDefaultResponse());
    }

    private ResourcesLoader getLoaderWithBasePath(final String basePath) {
        this.resourcesLoader.setBasePath(basePath);
        return this.resourcesLoader;
    }
}
