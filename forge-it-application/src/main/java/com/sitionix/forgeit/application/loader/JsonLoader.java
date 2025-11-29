package com.sitionix.forgeit.application.loader;

import java.io.IOException;
import java.io.InputStream;

public class JsonLoader {

    private static final String PATH_EXPECTED = "forge-it%s";

    public static String load(final String fileName) {
        final String filePath = String.format(PATH_EXPECTED, fileName);

        try (final InputStream isLoader = JsonLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            assert isLoader != null;
            return new String(isLoader.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to get file: %s", fileName));
        }
    }
}
