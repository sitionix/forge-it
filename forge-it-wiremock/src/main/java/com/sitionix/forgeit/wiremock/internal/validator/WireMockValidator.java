package com.sitionix.forgeit.wiremock.internal.validator;

import com.sitionix.forgeit.wiremock.internal.domain.WireMockCheck;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.IntSupplier;

import static com.sitionix.forgeit.wiremock.internal.validator.JsonMatcher.jsonEqualsIgnore;
import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class WireMockValidator {

    private final WireMockJournalClient journalClient;

    public void validate(final WireMockCheck<?, ?> check) {
        final IntSupplier liveCount = () -> this.findBodies(check).size();
        try {
            if (check.atLeastTimes() <= 0) {
                throw new IllegalArgumentException("atLeastTimes must be greater than zero");
            }
            this.verifyExactTimesWithBackoff(liveCount, check.atLeastTimes());
            if (nonNull(check.expectedJson())) {
                final String[] fieldsForIgnore = check.ignoredFields().toArray(new String[0]);
                final var actualJsons = this.findBodies(check);
                final boolean anyMatch = actualJsons.stream().anyMatch(jsonEqualsIgnore(check.expectedJson(), fieldsForIgnore));
                if (!anyMatch) {
                    throw new AssertionError("No matching JSON found for endpoint: " + check.endpoint());
                }
            }
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    private void verifyExactTimesWithBackoff(final IntSupplier actual, final int expected) {
        final int count = actual.getAsInt();
        if (count != expected) {
            throw new AssertionError("Request count is " + count + " but expected " + expected);
        }
    }

    private java.util.List<String> findBodies(final WireMockCheck<?, ?> check) {
        if (nonNull(check.id())) {
            return this.journalClient.findBodiesByStubMappingId(check.id());
        }
        return this.journalClient.findBodiesByUrl(check.endpoint());
    }
}
