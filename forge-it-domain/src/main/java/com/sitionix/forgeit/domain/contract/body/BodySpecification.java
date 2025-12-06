package com.sitionix.forgeit.domain.contract.body;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BodySpecification<E> {

    private String resourceName;
    private JsonBodySource source;
    private E entity;
    private Object id;


    public static <E> BodySpecification<E> defaultJsonName(final String resourceName) {
        return BodySpecification.<E>builder()
                .resourceName(resourceName)
                .source(JsonBodySource.JSON_DEFAULT)
                .build();
    }

    public static <E> BodySpecification<E> explicitJsonName(final String resourceName) {
        return BodySpecification.<E>builder()
                .resourceName(resourceName)
                .source(JsonBodySource.JSON)
                .build();
    }

    public static <E> BodySpecification<E> entityBody(final E entity) {
        return BodySpecification.<E>builder()
                .entity(entity)
                .source(JsonBodySource.ENTITY)
                .build();
    }

    public static <E> BodySpecification<E> getById(final Object id) {
        return BodySpecification.<E>builder()
                .id(id)
                .source(JsonBodySource.GET_BY_ID)
                .build();
    }
}
