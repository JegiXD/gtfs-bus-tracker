package com.gtfs.bustracker.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GtfsTimeUtilTest {

    @ParameterizedTest
    @CsvSource({
            "00:00:00, 0",
            "08:15:00, 29700",
            "23:59:59, 86399",
            // Časi po polnoči lahko pripadajo voznemu redu prejšnjega dne.
            "24:00:00, 86400",
            "25:30:00, 91800",
    })
    void parsesValidGtfsTimesToSecondsOfDay(String input, int expectedSeconds) {
        assertEquals(expectedSeconds, GtfsTimeUtil.parseToSecondsOfDay(input));
    }

    @Test
    void tolerantOfSurroundingWhitespace() {
        assertEquals(29700, GtfsTimeUtil.parseToSecondsOfDay("  08:15:00  "));
    }

    @Test
    void rejectsNullTime() {
        assertThrows(IllegalArgumentException.class, () -> GtfsTimeUtil.parseToSecondsOfDay(null));
    }

    @ParameterizedTest
    @CsvSource({
            "not-a-time",
            "08:15",
            "08:75:00",
            "08:15:75",
            ":15:00",
    })
    void rejectsMalformedTimes(String input) {
        assertThrows(IllegalArgumentException.class, () -> GtfsTimeUtil.parseToSecondsOfDay(input));
    }

    @Test
    void formatsRelativeMinutes() {
        assertEquals("0min", GtfsTimeUtil.formatRelativeMinutes(0));
        assertEquals("7min", GtfsTimeUtil.formatRelativeMinutes(7));
        assertEquals("120min", GtfsTimeUtil.formatRelativeMinutes(120));
    }

    @Test
    void clampsNegativeMinutesToZero() {
        assertEquals("0min", GtfsTimeUtil.formatRelativeMinutes(-5));
    }
}
