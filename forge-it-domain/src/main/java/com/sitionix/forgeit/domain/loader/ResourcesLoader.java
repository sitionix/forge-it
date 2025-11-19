package com.sitionix.forgeit.domain.loader;

public interface ResourcesLoader {

    <T> T getFromFile(String fileName, Class<T> tClass);

    String getFromFile(String fileName);
}
