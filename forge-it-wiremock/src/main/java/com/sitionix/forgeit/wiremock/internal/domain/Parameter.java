package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Parameter {

    private enum Type {
        EQUAL_TO,
        EQUAL_TO_IGNORE_CASE,
        CONTAINS,
        DOES_NOT_CONTAIN,
        MATCHES,
        DOES_NOT_MATCH,
        ABSENT,
        ANYTHING
    }

    private final Type type;
    private final String value;

    private Parameter(final Type type, final String value) {
        this.type = type;
        this.value = value;
    }

    public static Parameter equalTo(final String value) {
        return new Parameter(Type.EQUAL_TO, value);
    }

    public static Parameter equalToIgnoreCase(final String value) {
        return new Parameter(Type.EQUAL_TO_IGNORE_CASE, value);
    }

    public static Parameter contains(final String value) {
        return new Parameter(Type.CONTAINS, value);
    }

    public static Parameter doesNotContain(final String value) {
        return new Parameter(Type.DOES_NOT_CONTAIN, value);
    }

    public static Parameter matches(final String value) {
        return new Parameter(Type.MATCHES, value);
    }

    public static Parameter doesNotMatch(final String value) {
        return new Parameter(Type.DOES_NOT_MATCH, value);
    }

    public static Parameter absent() {
        return new Parameter(Type.ABSENT, null);
    }

    public static Parameter anything() {
        return new Parameter(Type.ANYTHING, null);
    }

    public StringValuePattern toPattern() {
        return switch (this.type) {
            case EQUAL_TO -> WireMock.equalTo(this.value);
            case EQUAL_TO_IGNORE_CASE -> WireMock.equalToIgnoreCase(this.value);
            case CONTAINS -> WireMock.containing(this.value);
            case DOES_NOT_CONTAIN -> WireMock.notContaining(this.value);
            case MATCHES -> WireMock.matching(this.value);
            case DOES_NOT_MATCH -> WireMock.notMatching(this.value);
            case ABSENT -> WireMock.absent();
            case ANYTHING -> WireMock.matching(".*");
        };
    }
}

