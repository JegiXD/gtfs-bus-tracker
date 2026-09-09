package com.gtfs.bustracker.service;

import com.gtfs.bustracker.model.Route;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArrivalFormatterTest {

    private final ArrivalFormatter formatter = new ArrivalFormatter();
    private final LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0);

    @Test
    void formatsAbsoluteTimes() {
        Route route = new Route("101", "101", "Downtown Loop");
        RouteArrivals arrivals = new RouteArrivals(route, List.of(
                LocalDateTime.of(2024, 3, 4, 8, 9),
                LocalDateTime.of(2024, 3, 4, 8, 20)));

        String line = formatter.format(arrivals, now, ArrivalFormatter.TimeFormat.ABSOLUTE);

        assertEquals("101: 08:09, 08:20", line);
    }

    @Test
    void formatsRelativeTimes() {
        Route route = new Route("101", "101", "Downtown Loop");
        RouteArrivals arrivals = new RouteArrivals(route, List.of(
                LocalDateTime.of(2024, 3, 4, 8, 9),
                LocalDateTime.of(2024, 3, 4, 8, 20)));

        String line = formatter.format(arrivals, now, ArrivalFormatter.TimeFormat.RELATIVE);

        assertEquals("101: 9min, 20min", line);
    }

    @Test
    void fallsBackToLongNameWhenShortNameMissing() {
        Route route = new Route("101", null, "Downtown Loop");
        RouteArrivals arrivals = new RouteArrivals(route, List.of(LocalDateTime.of(2024, 3, 4, 8, 9)));

        String line = formatter.format(arrivals, now, ArrivalFormatter.TimeFormat.ABSOLUTE);

        assertEquals("Downtown Loop: 08:09", line);
    }

    @Test
    void parsesTimeFormatCaseInsensitively() {
        assertEquals(ArrivalFormatter.TimeFormat.RELATIVE, ArrivalFormatter.TimeFormat.parse("Relative"));
        assertEquals(ArrivalFormatter.TimeFormat.ABSOLUTE, ArrivalFormatter.TimeFormat.parse("ABSOLUTE"));
    }

    @Test
    void rejectsInvalidTimeFormat() {
        assertThrows(IllegalArgumentException.class, () -> ArrivalFormatter.TimeFormat.parse("soon"));
    }
}
