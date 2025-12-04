package com.sitionix.forgeit.domain.loader;

public interface JsonLoader {

    <T> T getFromFile(String fileName, Class<T> tClass);

    String getFromFile(String fileName);

    void setBasePath(String basePath);
}
