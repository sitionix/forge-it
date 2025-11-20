package com.sitionix.forgeit.wiremock.internal.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class JsonLoader {

    private static final String ROOT_PATH = "json";

    private JsonLoader() {
    }

    static String load(final String relativePath) {
        final String filePath = buildFilePath(relativePath);
        try (final InputStream resourceStream = JsonLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (resourceStream == null) {
                throw new IllegalStateException(String.format("Failed to get file: %s", relativePath));
            }
            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to get file: %s", relativePath), e);
        }
    }

    private static String buildFilePath(final String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("File name must not be null or blank");
        }
        final String sanitizedPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return String.format("%s/%s", ROOT_PATH, sanitizedPath);
    }
}
