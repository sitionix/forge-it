package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.NegativeContainsPattern;
import com.github.tomakehurst.wiremock.matching.NegativeRegexPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WireMockValuePattern {

    private String equalTo;
    private Boolean caseInsensitive;
    private String contains;
    private String doesNotContain;
    private String matches;
    private String doesNotMatch;
    private Boolean absent;
    private String anything;

    private WireMockValuePattern() {
    }

    public static WireMockValuePattern equalTo(final String value) {
        final WireMockValuePattern pattern = new WireMockValuePattern();
        pattern.equalTo = value;
        return pattern;
    }

    public static WireMockValuePattern from(final StringValuePattern pattern) {
        if (pattern instanceof EqualToPattern equalToPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.equalTo = equalToPattern.getEqualTo();
            if (Boolean.TRUE.equals(equalToPattern.getCaseInsensitive())) {
                valuePattern.caseInsensitive = true;
            }
            return valuePattern;
        }
        if (pattern instanceof ContainsPattern containsPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.contains = containsPattern.getContains();
            return valuePattern;
        }
        if (pattern instanceof NegativeContainsPattern negativeContainsPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.doesNotContain = negativeContainsPattern.getDoesNotContain();
            return valuePattern;
        }
        if (pattern instanceof RegexPattern regexPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.matches = regexPattern.getMatches();
            return valuePattern;
        }
        if (pattern instanceof NegativeRegexPattern negativeRegexPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.doesNotMatch = negativeRegexPattern.getDoesNotMatch();
            return valuePattern;
        }
        if (pattern instanceof AbsentPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.absent = true;
            return valuePattern;
        }
        if (pattern instanceof AnythingPattern) {
            final WireMockValuePattern valuePattern = new WireMockValuePattern();
            valuePattern.anything = "anything";
            return valuePattern;
        }
        return equalTo(pattern.getExpected());
    }
}
