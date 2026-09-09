package com.gtfs.bustracker.service;

import com.gtfs.bustracker.gtfs.GtfsRepository;
import com.gtfs.bustracker.model.Route;
import com.gtfs.bustracker.model.ServiceCalendar;
import com.gtfs.bustracker.model.Stop;
import com.gtfs.bustracker.model.StopTimeEntry;
import com.gtfs.bustracker.model.TripInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NextArrivalsServiceTest {

    private static final Stop STOP = new Stop(1, "Test Stop");
    private static final LocalDate SERVICE_START = LocalDate.of(2024, 1, 1);
    private static final LocalDate SERVICE_END = LocalDate.of(2024, 12, 31);

    private final NextArrivalsService service = new NextArrivalsService();

    /** Ustvari testne podatke iz trojic: ID vožnje, ID linije in čas prihoda. */
    private static GtfsRepository repositoryOf(List<String[]> tripRouteArrivalRows) {
        List<StopTimeEntry> stopTimes = new ArrayList<>();
        Map<String, TripInfo> trips = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();
        Map<String, ServiceCalendar> calendars = new HashMap<>();

        boolean[] everyDay = {true, true, true, true, true, true, true};
        calendars.put("SVC1", new ServiceCalendar("SVC1", everyDay, SERVICE_START, SERVICE_END));

        int seq = 1;
        for (String[] row : tripRouteArrivalRows) {
            String tripId = row[0];
            String routeId = row[1];
            String gtfsTime = row[2];
            int seconds = com.gtfs.bustracker.util.GtfsTimeUtil.parseToSecondsOfDay(gtfsTime);
            stopTimes.add(new StopTimeEntry(tripId, seconds, seq++));
            trips.put(tripId, new TripInfo(tripId, routeId, "SVC1", "Headsign " + routeId));
            routes.putIfAbsent(routeId, new Route(routeId, routeId, "Route " + routeId + " Long"));
        }
        return GtfsRepository.forData(STOP, stopTimes, trips, routes, calendars);
    }

    @Test
    void onlyReturnsArrivalsWithinTwoHourWindow() {
        GtfsRepository repo = repositoryOf(List.of(
                new String[]{"T1", "101", "08:00:00"},  // Zdaj: vključeno.
                new String[]{"T2", "101", "09:59:00"},  // Manj kot dve uri: vključeno.
                new String[]{"T3", "101", "10:01:00"},  // Več kot dve uri: izključeno.
                new String[]{"T4", "101", "07:59:00"}   // Pred eno minuto: izključeno.
        ));
        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0);

        List<RouteArrivals> result = service.getNextArrivals(repo, now, 10);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getArrivalTimes().size());
        assertEquals(LocalDateTime.of(2024, 3, 4, 8, 0), result.get(0).getArrivalTimes().get(0));
        assertEquals(LocalDateTime.of(2024, 3, 4, 9, 59), result.get(0).getArrivalTimes().get(1));
    }

    @Test
    void limitsResultsToMaxPerRoute() {
        GtfsRepository repo = repositoryOf(List.of(
                new String[]{"T1", "101", "08:00:00"},
                new String[]{"T2", "101", "08:10:00"},
                new String[]{"T3", "101", "08:20:00"}
        ));
        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0);

        List<RouteArrivals> result = service.getNextArrivals(repo, now, 2);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getArrivalTimes().size());
        assertEquals(LocalDateTime.of(2024, 3, 4, 8, 0), result.get(0).getArrivalTimes().get(0));
        assertEquals(LocalDateTime.of(2024, 3, 4, 8, 10), result.get(0).getArrivalTimes().get(1));
    }

    @Test
    void groupsByRouteAndOrdersRoutesBySoonestArrival() {
        GtfsRepository repo = repositoryOf(List.of(
                new String[]{"T1", "101", "09:00:00"},
                new String[]{"T2", "102", "08:05:00"},
                new String[]{"T3", "102", "08:30:00"}
        ));
        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0);

        List<RouteArrivals> result = service.getNextArrivals(repo, now, 5);

        assertEquals(2, result.size());
        assertEquals("102", result.get(0).getRoute().getRouteId());
        assertEquals("101", result.get(1).getRoute().getRouteId());
    }

    @Test
    void routesWithNoUpcomingArrivalsAreOmitted() {
        GtfsRepository repo = repositoryOf(List.<String[]>of(
                new String[]{"T1", "101", "23:00:00"} // Zunaj časovnega okna.
        ));
        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0);

        List<RouteArrivals> result = service.getNextArrivals(repo, now, 5);

        assertTrue(result.isEmpty());
    }

    @Test
    void handlesTripsThatCrossMidnightViaPreviousServiceDay() {
        // Včerajšnja vožnja prispe danes ob 00:30.
        // V GTFS je ta čas zapisan kot 24:30:00.
        GtfsRepository repo = repositoryOf(List.<String[]>of(
                new String[]{"T1", "101", "24:30:00"}
        ));
        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 0, 0); // Današnja polnoč.

        List<RouteArrivals> result = service.getNextArrivals(repo, now, 5);

        assertEquals(1, result.size());
        assertEquals(LocalDateTime.of(2024, 3, 4, 0, 30), result.get(0).getArrivalTimes().get(0));
    }

    @Test
    void serviceInactiveOnQueryDateProducesNoArrivals() {
        Stop stop = new Stop(1, "Test Stop");
        List<StopTimeEntry> stopTimes = List.of(new StopTimeEntry("T1", 8 * 3600, 1));
        Map<String, TripInfo> trips = Map.of("T1", new TripInfo("T1", "101", "WEEKDAYS_ONLY", null));
        Map<String, Route> routes = Map.of("101", new Route("101", "101", null));
        // Vozni red na datum poizvedbe ne velja.
        Map<String, ServiceCalendar> calendars = Map.of("WEEKDAYS_ONLY",
                new ServiceCalendar("WEEKDAYS_ONLY", new boolean[]{true, true, true, true, true, false, false},
                        LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)));
        GtfsRepository repo = GtfsRepository.forData(stop, stopTimes, trips, routes, calendars);

        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0); // Datum zunaj veljavnosti voznega reda.
        List<RouteArrivals> result = service.getNextArrivals(repo, now, 5);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsNonPositiveMaxPerRoute() {
        GtfsRepository repo = repositoryOf(List.of());
        LocalDateTime now = LocalDateTime.of(2024, 3, 4, 8, 0);

        assertThrows(IllegalArgumentException.class, () -> service.getNextArrivals(repo, now, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getNextArrivals(repo, now, -1));
    }
}
