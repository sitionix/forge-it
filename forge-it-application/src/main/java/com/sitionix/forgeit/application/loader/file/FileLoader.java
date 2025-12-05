package com.sitionix.forgeit.application.loader.file;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.isNull;

public class FileLoader {

    private static final String PATH_EXPECTED = "forge-it%s";

    public static String load(final String fileName) {
        final String filePath = String.format(PATH_EXPECTED, fileName);

        try (final InputStream isLoader = FileLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (isNull(isLoader)) {
                throw new IllegalStateException(String.format("File not found: %s", filePath));
            }
            return new String(isLoader.readAllBytes());
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Failed to get file: %s", fileName));
        }
    }
}
