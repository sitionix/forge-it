package com.sitionix.forgeit.domain.delegator;

import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ScopedResourcesLoader {

    private final ResourcesLoader resourcesLoader;

    protected abstract String getResourcePath();

    public <T> T getFromFile(final String fileName, final Class<T> tClass) {
        return resourcesLoader.getFromFile(this.getFullPath(fileName), tClass);
    }

    public String getFromFile(final String fileName) {
        return resourcesLoader.getFromFile(this.getFullPath(fileName));
    }

    private String getFullPath(final String fileName) {
        return String.format("%s/%s", this.getResourcePath(), fileName);
    }

}
