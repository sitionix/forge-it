package com.sitionix.forgeit.wiremock.internal.validator;


import lombok.experimental.UtilityClass;

import java.util.function.Predicate;

@UtilityClass
public class JsonMatcher {

    public static Predicate<String> jsonEqualsIgnore(final String expectedJson, final String... fieldsForIgnore) {
        return jsonEqualsIgnore(expectedJson, true, fieldsForIgnore);
    }

    public static Predicate<String> jsonEqualsIgnore(final String expectedJson,
                                                     final boolean strict,
                                                     final String... fieldsForIgnore) {
        return actualJson -> {
            try {
                JsonComparator.compareJson(expectedJson, actualJson, strict, fieldsForIgnore);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }
}
