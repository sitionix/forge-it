package com.sitionix.forgeit.domain.delegator;

import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ScopedResourcesLoader {

    private final ResourcesLoader resourcesLoader;

    protected abstract String getResourcePath();

    public <T> T getFromFile(String fileName, Class<T> tClass) {
        return resourcesLoader.getFromFile(this.getResourcePath(), tClass);
    }
}
