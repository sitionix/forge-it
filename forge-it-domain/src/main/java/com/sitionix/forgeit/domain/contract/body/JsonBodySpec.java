package com.sitionix.forgeit.domain.contract.body;

public record JsonBodySpec(String resourceName,
                           JsonBodySource source) {

    public static JsonBodySpec defaultBody(final String resourceName) {
        return new JsonBodySpec(resourceName, JsonBodySource.DEFAULT);
    }

    public static JsonBodySpec explicitBody(final String resourceName) {
        return new JsonBodySpec(resourceName, JsonBodySource.EXPLICIT);
    }
}
