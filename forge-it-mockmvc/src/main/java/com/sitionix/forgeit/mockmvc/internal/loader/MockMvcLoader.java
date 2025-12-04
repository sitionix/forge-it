package com.sitionix.forgeit.mockmvc.internal.loader;

import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockMvcLoader {

    private final JsonLoader jsonLoader;

    private final MockMvcProperties props;

    public JsonLoader mvcRequest() {
        return this.getLoaderWithBasePath(this.props.getPath().getRequest());
    }

    public JsonLoader mvcResponse() {
        return this.getLoaderWithBasePath(this.props.getPath().getResponse());
    }

    public JsonLoader mvcDefaultRequest() {
        return this.getLoaderWithBasePath(this.props.getPath().getDefaultRequest());
    }

    public JsonLoader mvcDefaultResponse() {
        return this.getLoaderWithBasePath(this.props.getPath().getDefaultResponse());
    }

    private JsonLoader getLoaderWithBasePath(final String basePath) {
        this.jsonLoader.setBasePath(basePath);
        return this.jsonLoader;
    }
}
