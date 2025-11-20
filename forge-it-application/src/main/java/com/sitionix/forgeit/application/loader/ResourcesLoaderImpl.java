package com.sitionix.forgeit.application.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.sitionix.forgeit.application.loader.JsonLoader.load;

@Component
public class ResourcesLoaderImpl implements ResourcesLoader {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public <T> T getFromFile(String fileName, Class<T> tClass) {
        final String file = this.loadResource(fileName);
        return this.getResourceAsObject(file, tClass);
    }

    @Override
    public String getFromFile(String fileName) {
        return this.loadResource(fileName);
    }

    private <T> T getResourceAsObject(final String jsonResource, final Class<T> tClass) {
        try {
            return this.objectMapper.readValue(jsonResource, tClass);
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Failed parse from json: %s", jsonResource), e);
        }
    }

    private String loadResource(final String fileName) {
        return load("/%s", fileName);
    }
}
