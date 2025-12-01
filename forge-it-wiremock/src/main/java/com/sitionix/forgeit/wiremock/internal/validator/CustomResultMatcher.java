package com.sitionix.forgeit.wiremock.internal.validator;

import lombok.experimental.UtilityClass;
import org.springframework.test.web.servlet.ResultMatcher;

@UtilityClass
public class CustomResultMatcher {

    public static ResultMatcher jsonEqualsIgnore(final String expectedJson, final String... fieldsForIgnore) {
        return result -> JsonComparator.compareJson(expectedJson, result.getResponse().getContentAsString(), fieldsForIgnore);
    }
}
