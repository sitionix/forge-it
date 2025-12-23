package com.sitionix.forgeit.application.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityJsonComparatorTests {

    @Test
    void shouldMatchNumericValuesAcrossNodeTypes() {
        final SampleEntity actual = new SampleEntity(0L);
        assertDoesNotThrow(() -> EntityJsonComparator.assertMatchesJson(
                actual,
                "{\"retryCount\":0}",
                null));
    }

    @Test
    void shouldFailWhenNumericValuesDiffer() {
        final SampleEntity actual = new SampleEntity(1L);
        assertThrows(AssertionError.class, () -> EntityJsonComparator.assertMatchesJson(
                actual,
                "{\"retryCount\":0}",
                null));
    }

    @Test
    void givenMissingFieldInExpectedJsonWhenStrictMatchThenThrowsAssertionError() {
        final StrictEntity actual = new StrictEntity("alpha", 7);
        assertThrows(AssertionError.class, () -> EntityJsonComparator.assertMatchesJsonStrict(
                actual,
                "{\"name\":\"alpha\"}",
                null));
    }

    static final class SampleEntity {
        private final Long retryCount;

        SampleEntity(final Long retryCount) {
            this.retryCount = retryCount;
        }

        public Long getRetryCount() {
            return this.retryCount;
        }
    }

    static final class StrictEntity {
        private final String name;
        private final int count;

        StrictEntity(final String name, final int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return this.name;
        }

        public int getCount() {
            return this.count;
        }
    }
}
