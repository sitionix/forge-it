package com.sitionix.forgeit.application.loader.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.application.loader.file.FileLoader;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonLoaderImpl implements JsonLoader {

    private final ObjectMapper objectMapper;

    private String basePath;

    @Override
    public <T> T getFromFile(String fileName, Class<T> tClass) {
        final String file = this.loadResource(fileName);
        return this.getResourceAsObject(file, tClass);
    }

    @Override
    public String getFromFile(String fileName) {
        return this.loadResource(fileName);
    }

    @Override
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    private <T> T getResourceAsObject(final String jsonResource, final Class<T> tClass) {
        try {
            return this.objectMapper.readValue(jsonResource, tClass);
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Failed parse from json: %s", jsonResource), e);
        }
    }

    private String loadResource(final String fileName) {
        return FileLoader.load(this.basePath + "/" + fileName);
    }
}
