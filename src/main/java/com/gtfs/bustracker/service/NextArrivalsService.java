package com.gtfs.bustracker.service;

import com.gtfs.bustracker.gtfs.GtfsRepository;
import com.gtfs.bustracker.model.Route;
import com.gtfs.bustracker.model.StopTimeEntry;
import com.gtfs.bustracker.model.TripInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes, for a given stop, the next upcoming arrivals per route within a
 * fixed 2-hour lookahead window from a reference "now" instant.
 *
 * <p>GTFS allows arrival times to exceed 24:00:00 to represent a time after
 * midnight that is still logically part of the previous calendar day's
 * service (e.g. a bus scheduled at "25:30:00" departs at 01:30 the next
 * day, but as part of yesterday's timetable/service_id). To handle trips
 * that cross midnight correctly, this service checks both:
 * <ul>
 *   <li>services active on "today" (matched against the raw seconds-of-day
 *       directly), and</li>
 *   <li>services active on "yesterday" (matched against seconds-of-day
 *       shifted by +24h, to catch rows like "25:30:00").</li>
 * </ul>
 */
public final class NextArrivalsService {

    /** The application only ever looks ahead this far. */
    public static final long LOOKAHEAD_HOURS = 2;

    /**
     * Returns the next arrivals per route at the repository's stop, within
     * the next {@value #LOOKAHEAD_HOURS} hours of {@code now}, limited to
     * {@code maxPerRoute} arrivals per route.
     *
     * @param repository  stop-scoped GTFS data (see {@link GtfsRepository})
     * @param now          the reference query instant
     * @param maxPerRoute  maximum number of arrivals to return per route (must be &gt; 0)
     * @return arrivals grouped by route, sorted by each route's earliest arrival first;
     *         routes with no upcoming arrivals are omitted
     */
    public List<RouteArrivals> getNextArrivals(GtfsRepository repository, LocalDateTime now, int maxPerRoute) {
        if (maxPerRoute <= 0) {
            throw new IllegalArgumentException("maxPerRoute must be positive, got " + maxPerRoute);
        }

        long windowStartSec = now.toLocalTime().toSecondOfDay();
        long windowEndSec = windowStartSec + LOOKAHEAD_HOURS * 3600;

        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime todayMidnight = today.atStartOfDay();
        LocalDateTime yesterdayMidnight = yesterday.atStartOfDay();

        // routeId -> accumulated arrival instants (not yet sorted/limited)
        Map<String, List<LocalDateTime>> arrivalsByRoute = new LinkedHashMap<>();

        for (StopTimeEntry entry : repository.getStopTimesAtStop()) {
            TripInfo trip = repository.getTripsById().get(entry.getTripId());
            if (trip == null || trip.getRouteId() == null) {
                continue; // trip not resolvable - skip defensively
            }

            LocalDateTime arrivalInstant = null;

            // Case 1: this row belongs to a service running "today".
            if (repository.isServiceActiveOn(trip.getServiceId(), today)
                    && entry.getArrivalSeconds() >= windowStartSec
                    && entry.getArrivalSeconds() <= windowEndSec) {
                arrivalInstant = todayMidnight.plusSeconds(entry.getArrivalSeconds());
            }
            // Case 2: this row belongs to "yesterday's" service but the raw
            // time (>= 24:00:00) rolls into today's early-morning window.
            else if (repository.isServiceActiveOn(trip.getServiceId(), yesterday)
                    && entry.getArrivalSeconds() >= windowStartSec + 86400
                    && entry.getArrivalSeconds() <= windowEndSec + 86400) {
                arrivalInstant = yesterdayMidnight.plusSeconds(entry.getArrivalSeconds());
            }

            if (arrivalInstant != null) {
                arrivalsByRoute.computeIfAbsent(trip.getRouteId(), k -> new ArrayList<>()).add(arrivalInstant);
            }
        }

        List<RouteArrivals> result = new ArrayList<>();
        for (Map.Entry<String, List<LocalDateTime>> e : arrivalsByRoute.entrySet()) {
            List<LocalDateTime> times = e.getValue();
            times.sort(Comparator.naturalOrder());
            List<LocalDateTime> limited = times.size() > maxPerRoute
                    ? new ArrayList<>(times.subList(0, maxPerRoute))
                    : times;
            Route route = repository.getRoutesById().get(e.getKey());
            if (route == null) {
                route = new Route(e.getKey(), null, null);
            }
            result.add(new RouteArrivals(route, limited));
        }

        // Present routes ordered by their soonest upcoming arrival.
        result.sort(Comparator.comparing(ra -> ra.getArrivalTimes().get(0)));
        return result;
    }
}
