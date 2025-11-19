package com.sitionix.forgeit.application;

import com.sitionix.forgeit.domain.JsonLoader;
import org.springframework.stereotype.Service;

/**
 * Application-level implementation of the shared JSON loader contract.
 */
@Service
public class JsonLoaderImpl implements JsonLoader {

    @Override
    public String load() {
        return "application-layer-json";
    }
}
