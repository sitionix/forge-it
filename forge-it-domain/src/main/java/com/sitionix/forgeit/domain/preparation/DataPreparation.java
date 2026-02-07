package com.sitionix.forgeit.domain.preparation;

public interface DataPreparation<T> {
    void prepare(T forgeit) throws Exception;
}
